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

### Add Audit Logging

1. Create audit entity and adapter.
2. Inject into domain services via constructor.
3. Call audit method after each operation.

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
