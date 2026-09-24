# Receipt Upload & Auto-Itemize API

A Spring Boot API for receipt processing with automatic tax extraction and line item parsing.

## Architecture

This project uses **Hexagonal Architecture** (Ports & Adapters):

- **Domain Layer**: Core business logic, domain models, and port interfaces (zero Spring dependencies)
  - `domain/model/`: Domain entities (Receipt, Transaction, TaxLine, LineItem)
  - `domain/service/`: Business services (ReceiptProcessingService, ItemizationService)
  - `domain/port/`: Port interfaces (ReceiptRepository, TransactionRepository, OcrService, FileStorage)

- **Infrastructure Layer**: Database, file storage, and external service adapters
  - `infrastructure/persistence/`: JPA entities and Spring Data repositories
  - `infrastructure/adapter/`: Implementations of domain ports (JPA adapters, local file storage, OCR stubbing)
  - `infrastructure/config/`: Spring configuration for beans

- **Application Layer**: REST controllers and DTOs
  - `application/rest/`: REST endpoints and HTTP handling
  - `application/dto/`: Data transfer objects for API requests/responses

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+

### Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

### Configuration

Create `application.properties` or use environment variables:

```properties
# File upload directory (default: uploads)
upload.dir=uploads
```

## API Endpoints

### Health Check

```bash
curl http://localhost:8080/health
```

### 1. Upload Receipt

Upload a receipt image/PDF file:

```bash
curl -X POST \
  -F "file=@fixtures/task-a/receipt-clean.txt" \
  http://localhost:8080/receipts
```

Response:
```json
{
  "receiptId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Receipt uploaded successfully"
}
```

### 2. Process Receipt

Run OCR extraction and create transaction:

```bash
curl -X POST \
  http://localhost:8080/receipts/550e8400-e29b-41d4-a716-446655440000/process
```

Response:
```json
{
  "transactionId": "660e8400-e29b-41d4-a716-446655440001",
  "status": "COMPLETE"
}
```

### 3. Get Transaction

Retrieve transaction with taxes and line items:

```bash
curl http://localhost:8080/transactions/660e8400-e29b-41d4-a716-446655440001
```

Response:
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "receiptId": "550e8400-e29b-41d4-a716-446655440000",
  "merchant": "Acme Grocery Store",
  "date": "2024-12-15",
  "currency": "USD",
  "total": 21.55,
  "itemizeStatus": "COMPLETE",
  "taxes": [
    {
      "id": "770e8400-e29b-41d4-a716-446655440002",
      "name": "VAT",
      "rate": 20.0,
      "amount": 3.59,
      "jurisdiction": null
    }
  ],
  "items": [
    {
      "id": "880e8400-e29b-41d4-a716-446655440003",
      "description": "Milk 2L",
      "amount": 3.99,
      "quantity": 1.0,
      "taxAmount": null
    },
    {
      "id": "880e8400-e29b-41d4-a716-446655440004",
      "description": "Bread Whole Wheat",
      "amount": 2.49,
      "quantity": 1.0,
      "taxAmount": null
    }
  ]
}
```

### 4. Re-Itemize from Stored OCR

Re-run auto-itemize using stored OCR text:

```bash
curl -X POST \
  http://localhost:8080/transactions/660e8400-e29b-41d4-a716-446655440001/itemize
```

Response: Same as Get Transaction endpoint

### 5. Update Items (User Override)

Edit, merge, or split line items with validation:

```bash
curl -X PATCH \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "id": "880e8400-e29b-41d4-a716-446655440003",
        "description": "Milk 2L",
        "amount": 3.99,
        "quantity": 1.0,
        "taxAmount": null
      },
      {
        "id": "880e8400-e29b-41d4-a716-446655440004",
        "description": "Bread & Cheese",
        "amount": 8.48,
        "quantity": 1.0,
        "taxAmount": null
      }
    ]
  }' \
  http://localhost:8080/transactions/660e8400-e29b-41d4-a716-446655440001/items
```

**Error Response (409 - Reconciliation Mismatch):**
```json
{
  "error": "RECONCILIATION_MISMATCH",
  "message": "Items total (15.96) + taxes (3.59) = 19.55, but transaction total is 21.55"
}
```

## Test Scenarios

Three fixture files test different scenarios:

### Scenario 1: Clean Receipt with Items and VAT
- File: `fixtures/task-a/receipt-clean.txt`
- Status: `COMPLETE` (items + taxes reconcile)
- Items: 4 products
- Tax: 20% VAT

### Scenario 2: Tax-Only Receipt
- File: `fixtures/task-a/receipt-tax-only.txt`
- Status: `FAILED` (no line items extracted)
- Items: None
- Tax: 15% GST

### Scenario 3: Items Mismatch
- File: `fixtures/task-a/receipt-mismatch.txt`
- Status: `NEEDS_REVIEW` (items sum doesn't equal total)
- Items: 3 products
- Tax: 10% sales tax
- Note: Total is $60, but items + tax = $53.35

## OCR Implementation

**This implementation uses OCR stubbing**: The `StubOcrService` reads fixture text files instead of calling an OCR vendor. This is sufficient for validating the data model and API structure against the fixtures.

To integrate with a real OCR vendor (OpenAI Vision, Google Gemini, AWS Textract):
1. Inject API key via environment variable
2. Replace `StubOcrService` implementation in `LocalFileStorage` adapter
3. Call vendor API in the `extractText()` method

## Database

Uses **H2 in-memory database** for testing. Configuration in `application.properties`:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

For persistence across restarts, modify the URL to point to a file:
```properties
spring.datasource.url=jdbc:h2:file:./data/receipts
```

## Data Model

### Transaction Header
- `id`: UUID
- `receiptId`: Link to uploaded receipt
- `merchant`: Store/vendor name
- `date`: Transaction date
- `currency`: ISO code (USD, EUR, GBP)
- `total`: Grand total amount
- `itemizeStatus`: COMPLETE | NEEDS_REVIEW | FAILED

### Taxes (List)
- `id`: UUID
- `name`: Tax type (VAT, GST, Sales Tax)
- `rate`: Percentage
- `amount`: Calculated amount
- `jurisdiction`: Optional region

### Line Items (List)
- `id`: UUID
- `description`: Product name
- `amount`: Line total
- `quantity`: Unit quantity
- `taxAmount`: Item-level tax (optional)

### Receipt
- `id`: UUID
- `filePath`: Location of stored file
- `fileName`: Original filename
- `uploadedAt`: Timestamp
- `rawOcrText`: Extracted text (used for re-itemize)

## Error Handling

| Status | Error Code | Scenario |
|--------|-----------|----------|
| 400 | BAD_REQUEST | Empty file upload |
| 404 | NOT_FOUND | Receipt/Transaction not found |
| 409 | RECONCILIATION_MISMATCH | Items don't sum to total after user edits |
| 500 | UPLOAD_ERROR | File storage failure |
| 500 | PROCESS_ERROR | OCR extraction failed |
| 500 | UPDATE_ERROR | Item update error |

## Development Notes

- No authentication or authorization
- Local file storage (no cloud storage)
- In-memory/file-based SQLite option (H2)
- Regex-based OCR parsing (stub implementation)

## Running Tests

```bash
mvn test
```

## Project Structure

```
backend/
├── src/main/java/com/navan/task/backend/
│   ├── domain/
│   │   ├── model/          # Domain entities
│   │   ├── port/           # Port interfaces
│   │   └── service/        # Business logic
│   ├── infrastructure/
│   │   ├── adapter/        # Port implementations
│   │   ├── persistence/    # JPA entities
│   │   └── config/         # Spring beans
│   ├── application/
│   │   ├── rest/           # REST controllers
│   │   └── dto/            # Data transfer objects
│   └── BackendApplication.java
├── fixtures/task-a/
│   ├── receipt-clean.txt
│   ├── receipt-tax-only.txt
│   ├── receipt-mismatch.txt
│   └── gold.json
└── pom.xml
```

## License

MIT
