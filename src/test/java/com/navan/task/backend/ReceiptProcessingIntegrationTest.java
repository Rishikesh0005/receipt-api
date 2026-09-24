package com.navan.task.backend;

import com.navan.task.backend.domain.model.*;
import com.navan.task.backend.domain.port.OcrService;
import com.navan.task.backend.domain.port.TransactionRepository;
import com.navan.task.backend.domain.service.ReceiptProcessingService;
import com.navan.task.backend.infrastructure.adapter.StubOcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for receipt processing against fixture files.
 */
public class ReceiptProcessingIntegrationTest {

    private ReceiptProcessingService processingService;
    private InMemoryTransactionRepository repository;
    private OcrService ocrService;

    @BeforeEach
    public void setup() {
        repository = new InMemoryTransactionRepository();
        ocrService = new StubOcrService();
        processingService = new ReceiptProcessingService(repository, ocrService);
    }

    @Test
    public void testCleanReceipt() throws IOException {
        // Arrange
        String filePath = "fixtures/task-a/receipt-clean.txt";

        // Act
        Transaction transaction = processingService.processReceipt("receipt-1", filePath);

        // Assert
        assertEquals("Acme Grocery Store", transaction.getMerchant());
        assertEquals(LocalDate.of(2024, 12, 15), transaction.getDate());
        assertEquals("USD", transaction.getCurrency());
        assertEquals(new BigDecimal("21.55"), transaction.getTotal());

        assertEquals(Transaction.ItemizeStatus.COMPLETE, transaction.getItemizeStatus());

        // Taxes
        assertEquals(1, transaction.getTaxes().size());
        TaxLine tax = transaction.getTaxes().get(0);
        assertEquals("VAT", tax.getName());
        assertEquals(new BigDecimal("20"), tax.getRate());
        assertEquals(new BigDecimal("3.59"), tax.getAmount());

        // Items
        assertEquals(4, transaction.getItems().size());
        List<String> descriptions = transaction.getItems().stream()
                .map(LineItem::getDescription)
                .toList();
        assertTrue(descriptions.contains("Milk 2L"));
        assertTrue(descriptions.contains("Bread Whole Wheat"));
        assertTrue(descriptions.contains("Cheese Cheddar 1lb"));
        assertTrue(descriptions.contains("Eggs Dozen"));
    }

    @Test
    public void testTaxOnlyReceipt() throws IOException {
        // Arrange
        String filePath = "fixtures/task-a/receipt-tax-only.txt";

        // Act
        Transaction transaction = processingService.processReceipt("receipt-2", filePath);

        // Assert
        assertEquals("TechStore Downtown", transaction.getMerchant());
        assertEquals(LocalDate.of(2024, 3, 8), transaction.getDate());
        assertEquals("USD", transaction.getCurrency());
        assertEquals(new BigDecimal("172.50"), transaction.getTotal());
        assertEquals(Transaction.ItemizeStatus.FAILED, transaction.getItemizeStatus());

        // Should have tax
        assertEquals(1, transaction.getTaxes().size());
        TaxLine tax = transaction.getTaxes().get(0);
        assertEquals("GST", tax.getName());
        assertEquals(new BigDecimal("15"), tax.getRate());

        // No items extracted
        assertEquals(0, transaction.getItems().size());
    }

    @Test
    public void testMismatchReceipt() throws IOException {
        // Arrange
        String filePath = "fixtures/task-a/receipt-mismatch.txt";

        // Act
        Transaction transaction = processingService.processReceipt("receipt-3", filePath);

        // Assert
        assertEquals("Restaurant Deluxe", transaction.getMerchant());
        assertEquals("USD", transaction.getCurrency());
        assertEquals(new BigDecimal("60.00"), transaction.getTotal());
        assertEquals(Transaction.ItemizeStatus.NEEDS_REVIEW, transaction.getItemizeStatus());

        // Has items
        assertEquals(3, transaction.getItems().size());

        // Has tax
        assertTrue(transaction.getTaxes().size() >= 1);
    }

    /**
     * Simple in-memory transaction repository for testing.
     */
    public static class InMemoryTransactionRepository implements TransactionRepository {
        private final Map<String, Transaction> store = new HashMap<>();

        @Override
        public Transaction save(Transaction transaction) {
            store.put(transaction.getId(), transaction);
            return transaction;
        }

        @Override
        public Optional<Transaction> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Transaction> findByReceiptId(String receiptId) {
            return store.values().stream()
                    .filter(t -> receiptId.equals(t.getReceiptId()))
                    .findFirst();
        }

        @Override
        public List<Transaction> findAll() {
            return new ArrayList<>(store.values());
        }
    }
}
