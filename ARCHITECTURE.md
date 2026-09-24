# Architecture Overview

## Hexagonal Architecture (Ports & Adapters)

This application follows **Hexagonal Architecture** principles to ensure clean separation of concerns, testability, and independent deployment.

### Core Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                            │
│  (REST Controllers, DTOs, HTTP Handlers)                         │
│  ├── ReceiptController                                           │
│  └── TransactionController                                       │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│                      DOMAIN LAYER                               │
│  (Business Logic, Domain Models, Port Interfaces)               │
│  ├── Models                                                      │
│  │   ├── Receipt (receipt metadata)                             │
│  │   ├── Transaction (header + taxes + items)                   │
│  │   ├── TaxLine (tax detail)                                   │
│  │   └── LineItem (line item detail)                            │
│  ├── Services (Stateless, Spring-free)                          │
│  │   ├── ReceiptProcessingService (OCR → Transaction)           │
│  │   └── ItemizationService (item merge/split + validation)     │
│  └── Ports (Interfaces only, no implementation)                 │
│      ├── ReceiptRepository (domain interface)                   │
│      ├── TransactionRepository (domain interface)               │
│      ├── OcrService (input port - text extraction)              │
│      └── FileStorage (output port - file persistence)           │
└──────────────────────┬──────────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────────┐
│               INFRASTRUCTURE LAYER                              │
│  (Spring-aware adapters and implementations)                    │
│  ├── Adapters (Implement domain ports)                          │
│  │   ├── TransactionRepositoryAdapter (JPA bridge)              │
│  │   ├── ReceiptRepositoryAdapter (JPA bridge)                  │
│  │   ├── StubOcrService (fixture-based or API-based)            │
│  │   └── LocalFileStorage (disk or cloud storage)               │
│  ├── Persistence (JPA entities and repositories)                │
│  │   ├── TransactionEntity (JPA @Entity)                        │
│  │   ├── ReceiptEntity (JPA @Entity)                            │
│  │   ├── TaxLineEntity (JPA @Entity)                            │
│  │   ├── LineItemEntity (JPA @Entity)                           │
│  │   ├── JpaTransactionRepository (Spring Data)                 │
│  │   └── JpaReceiptRepository (Spring Data)                     │
│  └── Config (Spring DI setup)                                   │
│      └── DomainBeanConfig (Service instantiation)               │
└─────────────────────────────────────────────────────────────────┘
```

## Dependency Flow

```
Application Layer
    ↓
Domain Services
    ↓ (depend on)
Domain Ports (interfaces)
    ↓ (implemented by)
Infrastructure Adapters
    ↓
Database / External APIs
```

**Key Principle**: Domain layer has **zero dependencies** on Spring or infrastructure. Controllers depend on domain services through ports.

---

## Request Flow Example: Upload & Process Receipt

```
1. Client
   ↓
2. ReceiptController.uploadReceipt()
   ├─ Create Receipt domain model
   ├─ Call FileStorage adapter → store file on disk
   ├─ Call ReceiptRepository adapter → persist to DB
   └─ Return receiptId
   ↓
3. Client calls process endpoint
   ↓
4. ReceiptController.processReceipt()
   ├─ Fetch Receipt via ReceiptRepository adapter
   ├─ Call ReceiptProcessingService (domain service)
   │  ├─ Call OcrService adapter → extract text from file
   │  ├─ Parse OCR text (regex-based in stub, or real vendor)
   │  ├─ Create Transaction domain model
   │  ├─ Extract merchant, date, currency, total
   │  ├─ Extract taxes and line items
   │  ├─ Auto-itemize with status (COMPLETE/NEEDS_REVIEW/FAILED)
   │  └─ Return Transaction
   ├─ Call TransactionRepository adapter → persist to DB
   └─ Return transactionId + status
```

---

## Port Interfaces (Hexagon Boundaries)

### Input Ports (Driving Adapters)
- REST endpoints (ReceiptController, TransactionController)
- HTTP requests trigger domain services

### Output Ports (Driven Adapters)
1. **ReceiptRepository**: Persistence contract
   - `save(Receipt)` → Called after upload
   - `findById(String)` → Called when retrieving receipt for OCR

2. **TransactionRepository**: Persistence contract
   - `save(Transaction)` → Called after processing
   - `findById(String)` → Called when fetching transaction
   - `findByReceiptId(String)` → Called to find existing transaction for receipt

3. **OcrService**: Text extraction contract
   - `extractText(String filePath)` → Called by ReceiptProcessingService
   - Stub: reads local text file
   - Real: calls OpenAI Vision, Google Gemini, AWS Textract, etc.

4. **FileStorage**: File persistence contract
   - `store(MultipartFile, receiptId)` → Called during upload
   - Stub: local disk storage
   - Real: S3, Google Cloud Storage, Azure Blob Storage, etc.

---

## Data Model & Reconciliation

### Transaction Reconciliation Logic

A transaction is marked `COMPLETE` if:
```
sum(line_items.amount) + sum(taxes.amount) == transaction.total
```

If amounts don't match:
- Status → `NEEDS_REVIEW` (user can edit items via PATCH endpoint)
- When user updates items via PATCH, the service validates again
- If still mismatched → HTTP 409 (Conflict) with detailed error message
- Transaction is **not silently fixed** (no invented line items)

### Tax Extraction

Regex patterns match:
- `VAT 20%: $3.59` → name=VAT, rate=20%, amount=3.59
- `GST (15%): 22.50` → name=GST, rate=15%, amount=22.50
- `Sales Tax 10% = $4.85` → name=Sales Tax, rate=10%, amount=4.85

### Line Item Extraction

Simple heuristic: lines with a `$amount` at the end are items:
- `Milk 2L $3.99` → description="Milk 2L", amount=3.99
- Filters out tax, total, date, time lines by keyword

---

## Database Schema (H2)

```sql
CREATE TABLE receipts (
    id VARCHAR(36) PRIMARY KEY,
    file_path VARCHAR(500),
    file_name VARCHAR(200),
    uploaded_at TIMESTAMP,
    raw_ocr_text CLOB
);

CREATE TABLE transactions (
    id VARCHAR(36) PRIMARY KEY,
    receipt_id VARCHAR(36),
    merchant VARCHAR(200),
    date DATE,
    currency VARCHAR(3),
    total DECIMAL(12,2),
    itemize_status VARCHAR(20)
);

CREATE TABLE tax_lines (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36),
    name VARCHAR(100),
    rate DECIMAL(5,2),
    amount DECIMAL(12,2),
    jurisdiction VARCHAR(100)
);

CREATE TABLE line_items (
    id VARCHAR(36) PRIMARY KEY,
    transaction_id VARCHAR(36),
    description VARCHAR(200),
    amount DECIMAL(12,2),
    quantity DECIMAL(8,2),
    tax_amount DECIMAL(12,2)
);
```

---

## Extending the System

### Add a New OCR Vendor

1. Create adapter in `infrastructure/adapter/OpenAIVisionService.java`:
   ```java
   @Component
   public class OpenAIVisionService implements OcrService {
       public String extractText(String filePath) {
           // Call OpenAI Vision API
       }
   }
   ```

2. Domain services use it automatically via interface (no changes needed).

3. Switch adapters via Spring component scanning or configuration.

### Add New Validation Rules

1. Extend `ReceiptProcessingService.parseReceiptText()` or `ItemizationService`.
2. No REST controller changes needed (domain logic is independent).

### Audit Logging (implemented)

Every receipt upload, OCR process, re-itemize and item-patch call writes an entry
to the audit trail:

- Domain model: `ProcessingLog` (framework-free)
- Port: `ProcessingLogRepository` (`save`, `findRecent`, `findByEntityId`)
- Service: `AuditLogService` — the only place that writes logs; injected into
  `ReceiptUploadService`, `ReceiptIngestionService`, `ItemizeService`, `ItemPatchService`
- Adapter: `ProcessingLogRepositoryAdapter` (JPA-backed, table `processing_logs`)
- REST: `GET /logs?limit=100` (recent, all entities) and `GET /logs/{entityId}` (history for one receipt/transaction)
- UI: the "Activity log" panel on the test console polls this after every workflow run and PATCH

---

## Database Model (High Level)

The app uses a single relational schema (H2 in-memory locally; swap the datasource
for Postgres/MySQL in any other environment with **zero domain code changes**,
since persistence is entirely behind the `ReceiptRepository` / `TransactionRepository`
/ `ProcessingLogRepository` ports).

```
┌───────────────────────┐        ┌────────────────────────────┐
│       receipts        │        │       transactions          │
├───────────────────────┤        ├────────────────────────────┤
│ id            (PK)     │◄───┐   │ id              (PK)        │
│ original_filename      │    │   │ receipt_id      (FK, 1:1)   │───┘
│ file_path              │    └───┤ merchant                    │
│ raw_ocr_text  (TEXT)    │        │ date                        │
└───────────────────────┘        │ currency                    │
                                  │ total                        │
                                  │ itemize_status               │
                                  │   (COMPLETE|NEEDS_REVIEW|    │
                                  │    FAILED)                   │
                                  └───────────┬────────────┬────┘
                                              │            │
                              ┌───────────────▼─┐   ┌──────▼───────────┐
                              │    tax_lines     │   │    line_items     │
                              ├──────────────────┤   ├───────────────────┤
                              │ id        (PK)    │   │ id         (PK)    │
                              │ transaction_id(FK)│   │ transaction_id(FK) │
                              │ name              │   │ description        │
                              │ rate              │   │ amount             │
                              │ amount            │   │ quantity           │
                              │ jurisdiction      │   │ tax_amount         │
                              └──────────────────┘   └───────────────────┘

┌────────────────────────────────────────┐
│            processing_logs               │
├────────────────────────────────────────┤
│ id            (PK)                       │
│ entity_type    ("RECEIPT" | "TRANSACTION")│
│ entity_id      (references receipts.id    │
│                 or transactions.id,        │
│                 no FK constraint — logs    │
│                 must never block on a      │
│                 missing/deleted parent)    │
│ action         (UPLOAD, PROCESS, ITEMIZE,  │
│                 PATCH_ITEMS)               │
│ level          (INFO | WARN | ERROR)       │
│ message        (TEXT)                      │
│ timestamp                                  │
└────────────────────────────────────────┘
```

**Relationships**
- `receipts 1 ── 1 transactions` (one transaction per processed receipt; enforced by `ReceiptIngestionService` calling OCR/extraction exactly once)
- `transactions 1 ── N tax_lines`, `transactions 1 ── N line_items` (cascade save/delete, `orphanRemoval = true` — replacing items via PATCH deletes the old rows)
- `processing_logs` references `receipts.id` or `transactions.id` by convention only (no FK) — audit history must survive even if you later add hard-deletes for receipts/transactions

**Key modeling decisions**
- IDs are UUID strings generated in the domain layer (`Transaction`, `Receipt`, `ProcessingLog`), not DB-generated — so an id is known immediately after `new Transaction(...)`, before any save call, and it's preserved exactly across every persistence round-trip via each model's `restore(...)` factory
- Money fields (`total`, `amount`, `rate`) are `BigDecimal`/`DECIMAL` end-to-end — never `float`/`double` — to avoid cent-rounding bugs
- `itemize_status` is a real enum column (`@Enumerated(STRING)`), not a free-text status, so invalid states are impossible at the DB level
- `raw_ocr_text` is stored on the receipt (not re-derived) so `POST /transactions/{id}/itemize` can re-run extraction without re-reading the original file or calling OCR again

---

## Testing Strategy

- **Domain layer**: Pure unit tests, no Spring context (fast, isolated)
- **Adapter layer**: Integration tests with H2 database or test doubles
- **REST layer**: MockMvc tests or integration tests with embedded server

---

## Deployment Considerations

- Domain JAR can be reused in CLI, gRPC, or GraphQL applications
- Adapters are swappable (e.g., PostgreSQL instead of H2)
- OCR service can point to different vendors without rebuilding domain logic
