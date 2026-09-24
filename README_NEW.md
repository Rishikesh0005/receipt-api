# Receipt Processing Service

A Spring Boot microservice for receipt upload, OCR extraction, and automatic itemization with reconciliation.

## Architecture

This service implements **Hexagonal Architecture (Ports & Adapters)** with complete separation of concerns:

```
┌─ Domain Layer (Pure Business Logic)
│  ├─ Models: Receipt, Transaction, Tax, LineItem, Money
│  ├─ Services: ReconciliationService, ItemizationDomainService
│  └─ Ports: Repository, Storage, OCR interfaces
│
├─ Application Layer (Use Cases)
│  ├─ Services: UploadReceiptService, ProcessReceiptService, etc.
│  └─ Ports: Input (use cases) & Output (adapters)
│
├─ Infrastructure Layer (Technical Implementation)
│  ├─ Persistence: JPA entities and repositories
│  ├─ Storage: Local file system
│  ├─ OCR: Fixture-based stubbing
│  └─ Config: Spring beans and configuration
│
└─ Adapter Layer (HTTP)
   ├─ Controllers: REST endpoints
   ├─ DTOs: Request/Response models
   └─ Exception Handlers: Global error handling
```

### Key Design Principles

- **Domain-Driven Design**: Business logic lives in the domain layer, completely independent of frameworks
- **Dependency Inversion**: High-level modules depend on abstractions (ports), not low-level details
- **Single Responsibility**: Each class has one clear purpose
- **Testability**: Domain and application logic can be unit tested without Spring

## Business Rules

### Reconciliation Logic

A transaction is marked **COMPLETE** only if:
```
sum(line_items.amount) + sum(taxes.amount) == transaction.total
```

If items don't reconcile:
- Status → **NEEDS_REVIEW**
- Transaction is persisted (not discarded)
- No fake items are created
- User can manually edit items via PATCH endpoint

### Transaction Lifecycle

1. **Upload**: Receipt file stored locally
2. **Process**: OCR extracted, structured data created, ONE transaction per receipt
3. **Re-itemize**: Uses stored OCR, replaces items only, never creates new transaction
4. **Edit**: User can merge/split items, validation enforced before save

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+

### Build and Run

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

### Configuration

```properties
# File storage directory
upload.dir=./uploads

# H2 database (in-memory by default)
spring.datasource.url=jdbc:h2:mem:testdb
```

## API Endpoints

### 1. Health Check

```bash
curl http://localhost:8080/health
```

**Response:**
```json
"OK"
```

### 2. Upload Receipt

Multipart file upload with local storage.

```bash
curl -X POST \
  -F "file=@fixtures/task-a/receipt-clean.txt" \
  http://localhost:8080/receipts
```

**Response:**
```json
{
  "receipt_id": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Receipt uploaded successfully"
}
```

### 3. Process Receipt

Run OCR extraction and create transaction.

```bash
curl -X POST \
  http://localhost:8080/receipts/550e8400-e29b-41d4-a716-446655440000/process
```

**Response:**
```json
{
  "transaction_id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "COMPLETE"
}
```

### 4. Get Transaction

Retrieve transaction with all details.

```bash
curl http://localhost:8080/transactions/660e8400-e29b-41d4-a716-446655440001
```

**Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "receipt_id": "550e8400-e29b-41d4-a716-446655440000",
  "merchant": "Acme Grocery Store",
  "date": "2024-12-15",
  "currency": "USD",
  "grand_total": 21.55,
  "itemize_status": "COMPLETE",
  "taxes": [
    {
      "id": "770e8400...",
      "name": "VAT",
      "rate": 20.0,
      "amount": 3.59
    }
  ],
  "line_items": [
    {
      "id": "880e8400...",
      "description": "Milk 2L",
      "amount": 3.99
    }
  ]
}
```

### 5. Re-Itemize

Re-run itemization using stored OCR.

```bash
curl -X POST \
  http://localhost:8080/transactions/660e8400-e29b-41d4-a716-446655440001/itemize
```

### 6. Update Line Items

Edit/merge/split items with reconciliation validation.

```bash
curl -X PATCH \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "description": "Milk & Bread",
        "amount": 6.48
      },
      {
        "description": "Cheese & Eggs",
        "amount": 11.48
      }
    ]
  }' \
  http://localhost:8080/transactions/660e8400-e29b-41d4-a716-446655440001/items
```

**On Mismatch (409):**
```json
{
  "code": "ITEM_TOTAL_MISMATCH",
  "message": "Line items do not reconcile with transaction total",
  "details": {
    "expected": 21.55,
    "actual": 17.96,
    "difference": 3.59
  }
}
```

## Test Fixtures

Three fixtures test different scenarios:

### receipt-clean.txt (COMPLETE)
- 4 line items
- 1 tax line (20% VAT)
- Items + tax = total ✓

### receipt-tax-only.txt (FAILED)
- No useful line items
- 1 tax line (15% GST)
- Items: 0, Status: FAILED

### receipt-mismatch.txt (NEEDS_REVIEW)
- 3 line items
- 1 tax line (10% sales tax)
- Items + tax ≠ total (intentional mismatch)

Run fixture tests:

```bash
mvn test -Dtest=ReceiptProcessingIntegrationTest
```

## OCR Implementation

**OCR is stubbed using the provided fixtures.**

The `FixtureOcrAdapter` reads text files instead of calling external APIs. This is sufficient for validating the data model and API.

To integrate a real OCR vendor (OpenAI Vision, Google Gemini, AWS Textract):

1. Create adapter: `infrastructure/ocr/VendorOcrAdapter.java`
2. Implement `OcrPort` interface
3. Call vendor API in `extractText()` method
4. Swap adapter via Spring component scanning

No external API keys are required for this solution.

## Database

Uses **H2 in-memory database** for simplicity.

Schema:
- `receipts`: Receipt metadata + raw OCR
- `transactions`: Transaction headers
- `tax_lines`: Tax details (foreign key to transactions)
- `line_items`: Line item details (foreign key to transactions)

For persistence across restarts:

```properties
spring.datasource.url=jdbc:h2:file:./data/receipts
```

## Error Handling

Global exception handler returns consistent JSON errors:

| Status | Code | Scenario |
|--------|------|----------|
| 400 | INVALID_REQUEST | Malformed request or empty file |
| 404 | NOT_FOUND | Receipt or transaction not found |
| 409 | ITEM_TOTAL_MISMATCH | PATCH with non-reconciling items |
| 500 | INTERNAL_ERROR | Unexpected server errors |

Example error response:

```json
{
  "code": "NOT_FOUND",
  "message": "Transaction not found: 123",
  "timestamp": "2024-09-24T00:31:13+05:30"
}
```

## Running Tests

```bash
# All tests
mvn test

# Fixture integration tests only
mvn test -Dtest=ReceiptProcessingIntegrationTest

# With coverage
mvn test jacoco:report
```

## Code Structure

```
src/main/java/com/company/receipt/
├── domain/                    # Pure business logic
│   ├── model/                 # Aggregates & value objects
│   ├── service/               # Domain services
│   ├── port/                  # Abstractions
│   └── exception/             # Domain exceptions
├── application/               # Use cases & orchestration
│   ├── port/                  # Input/output ports
│   ├── service/               # Application services
│   └── dto/                   # Command/response DTOs
├── infrastructure/            # Technical adapters
│   ├── persistence/           # JPA & database
│   ├── storage/               # File storage
│   ├── ocr/                   # OCR vendor integration
│   └── config/                # Spring configuration
└── adapter/                   # HTTP layer
    └── in/web/                # Controllers & DTOs
```

## Design Decisions

### 1. Hexagonal Architecture
Chose hexagonal architecture for:
- Clear separation of business logic from infrastructure
- Easy to test (mock adapters)
- Easy to swap implementations (e.g., OCR vendor)
- Framework-independent domain

### 2. No LLM/OCR API Calls
The assignment explicitly allows stubbing OCR. We read fixture text files instead of calling external APIs because:
- No credentials or API keys required
- Fixture files are the source of truth
- Fast, deterministic testing
- Easy to swap with real OCR later

### 3. Regex-Based Extraction
Simple regex patterns for parsing OCR text because:
- Fixtures have predictable format
- No NLP/ML needed for this scope
- Easy to understand and modify
- Fast execution

### 4. BigDecimal for Money
Used `BigDecimal` (not `double` or `float`) because:
- Avoids floating-point precision errors
- Required for financial calculations
- Industry standard for monetary values
- Explicit rounding control

### 5. Single Transaction Per Receipt
Never create duplicate transactions because:
- Receipt is the source of truth
- Simplifies auditing and reconciliation
- Clear transaction identity
- Prevents data duplication

### 6. No Fake Items on Mismatch
Don't invent line items to force reconciliation because:
- Preserves data integrity
- Flags issues for manual review
- Prevents silent data corruption
- User has full visibility and control

## Future Enhancements

- [ ] Real OCR vendor integration (OpenAI Vision, Google Gemini)
- [ ] User authentication and multi-tenancy
- [ ] Audit logging
- [ ] Receipt image PDF support
- [ ] Manual item adjustment UI
- [ ] Bulk receipt processing
- [ ] Export to accounting systems
- [ ] Receipt search and filtering

## License

MIT
