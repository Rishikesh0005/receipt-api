package com.navan.task.backend.domain.service;

import com.navan.task.backend.domain.exception.OcrExtractionException;
import com.navan.task.backend.domain.model.*;
import com.navan.task.backend.domain.port.OcrService;
import com.navan.task.backend.domain.port.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Domain service for processing receipts and extracting transactions.
 * Contains core business logic for parsing OCR text and creating structured data.
 */
public class ReceiptProcessingService {
    private final TransactionRepository transactionRepository;
    private final OcrService ocrService;
    private final ReconciliationPolicy reconciliationPolicy = new ReconciliationPolicy();

    public ReceiptProcessingService(TransactionRepository transactionRepository, OcrService ocrService) {
        this.transactionRepository = transactionRepository;
        this.ocrService = ocrService;
    }

    /**
     * Extract OCR text from a receipt file.
     */
    public String extractOcrText(String filePath) {
        return ocrService.extractText(filePath);
    }

    /**
     * Process a receipt: extract text via OCR and create/update transaction.
     */
    public Transaction processReceipt(String receiptId, String filePath) {
        // Extract OCR text
        String ocrText = ocrService.extractText(filePath);
        if (ocrText == null || ocrText.trim().isEmpty()) {
            throw new OcrExtractionException("OCR extraction failed or returned empty text");
        }

        // Find or create transaction
        Transaction transaction = new Transaction(receiptId);

        // Parse OCR text
        ParsedReceipt parsed = parseReceiptText(ocrText);

        // Set header fields
        transaction.setMerchant(parsed.merchant);
        transaction.setDate(parsed.date);
        transaction.setCurrency(parsed.currency);
        transaction.setTotal(parsed.total);

        // Set taxes
        transaction.setTaxes(parsed.taxes);

        // Auto-itemize
        List<LineItem> items = parsed.items;
        transaction.setItems(items);

        // Determine itemize status
        if (parsed.items.isEmpty()) {
            transaction.setItemizeStatus(Transaction.ItemizeStatus.FAILED);
        } else if (itemsReconcile(items, parsed.taxes, transaction.getTotal())) {
            transaction.setItemizeStatus(Transaction.ItemizeStatus.COMPLETE);
        } else {
            transaction.setItemizeStatus(Transaction.ItemizeStatus.NEEDS_REVIEW);
        }

        // Persist
        Transaction saved = transactionRepository.save(transaction);
        return saved;
    }

    /**
     * Re-itemize using stored OCR text.
     */
    public void reItemize(Transaction transaction, String ocrText) {
        ParsedReceipt parsed = parseReceiptText(ocrText);

        // Replace line items
        transaction.setItems(parsed.items);

        // Recalculate status against the transaction's existing taxes
        if (parsed.items.isEmpty()) {
            transaction.setItemizeStatus(Transaction.ItemizeStatus.FAILED);
        } else if (itemsReconcile(parsed.items, transaction.getTaxes(), transaction.getTotal())) {
            transaction.setItemizeStatus(Transaction.ItemizeStatus.COMPLETE);
        } else {
            transaction.setItemizeStatus(Transaction.ItemizeStatus.NEEDS_REVIEW);
        }

        transactionRepository.save(transaction);
    }

    /**
     * Check if line items + taxes reconcile with the transaction total,
     * using the same policy applied to user PATCH edits.
     */
    private boolean itemsReconcile(List<LineItem> items, List<TaxLine> taxes, BigDecimal total) {
        return reconciliationPolicy.check(total, items, taxes).isReconciled();
    }

    /**
     * Parse OCR text into structured receipt data.
     */
    private ParsedReceipt parseReceiptText(String ocrText) {
        ParsedReceipt result = new ParsedReceipt();

        // Simple heuristic parsing (real OCR would be more sophisticated)
        // Extract merchant (first capitalized sequence or "Merchant")
        result.merchant = extractMerchant(ocrText);

        // Extract date
        result.date = extractDate(ocrText);

        // Default currency
        String lowerText = ocrText.toLowerCase();
        result.currency = "USD";
        if (lowerText.contains("eur") || lowerText.contains("€")) {
            result.currency = "EUR";
        } else if (lowerText.contains("gbp") || lowerText.contains("£")) {
            result.currency = "GBP";
        }

        // Extract taxes
        result.taxes = extractTaxes(ocrText);

        // Extract total
        result.total = extractTotal(ocrText);

        // Extract line items
        result.items = extractLineItems(ocrText);

        return result;
    }

    private String extractMerchant(String text) {
        // Prefer an explicit "MERCHANT: <name>" labeled field if present.
        Matcher labeled = Pattern.compile("(?im)^\\s*MERCHANT\\s*:\\s*(.+?)\\s*$").matcher(text);
        if (labeled.find()) {
            return labeled.group(1).trim();
        }

        // Try to find lines that might be merchant names
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 3 && trimmed.length() < 100) {
                // Skip lines that are clearly amounts or dates
                if (!trimmed.matches(".*\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}.*")
                        && !trimmed.matches(".*\\$\\s*\\d+.*")) {
                    return trimmed;
                }
            }
        }
        return "Unknown Merchant";
    }

    private LocalDate extractDate(String text) {
        // Try common date patterns
        Pattern[] patterns = {
                Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})"),
                Pattern.compile("(\\d{4})[/.-](\\d{1,2})[/.-](\\d{1,2})")
        };

        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                try {
                    String date = m.group(0);
                    // Try parsing as MM/DD/YYYY or DD/MM/YYYY
                    try {
                        return LocalDate.parse(date, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
                    } catch (Exception e1) {
                        try {
                            return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        } catch (Exception e2) {
                            // Try YYYY-MM-DD
                            return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        }
                    }
                } catch (Exception e) {
                    // Continue
                }
            }
        }
        return LocalDate.now();
    }

    private List<TaxLine> extractTaxes(String text) {
        List<TaxLine> taxes = new ArrayList<>();

        // Improved pattern: match tax name, rate, and amount
        // Examples: "VAT (20%): $3.59", "GST (15%): 22.50", "Tax 10% = $4.85"
        Pattern taxPattern = Pattern.compile(
                "(?:VAT|GST|Sales Tax|Tax)\\s*(?:\\(([0-9.]+)%?\\)|([0-9.]+)%)?\\s*[:\\(=]?\\s*\\$?([0-9.]+)",
                Pattern.CASE_INSENSITIVE
        );

        Matcher m = taxPattern.matcher(text);

        while (m.find()) {
            try {
                String fullMatch = m.group(0);
                
                // Extract tax name from the full match
                String taxName = "VAT"; // default
                if (fullMatch.toLowerCase().contains("gst")) {
                    taxName = "GST";
                } else if (fullMatch.toLowerCase().contains("sales tax")) {
                    taxName = "Sales Tax";
                } else if (fullMatch.toLowerCase().startsWith("tax") && !fullMatch.toLowerCase().contains("vat")) {
                    taxName = "Tax";
                }

                // Extract rate (group 1 or 2)
                String rateStr = m.group(1) != null ? m.group(1) : m.group(2);
                BigDecimal rate = rateStr != null ? new BigDecimal(rateStr) : BigDecimal.ZERO;

                // Extract amount (group 3)
                BigDecimal amount = new BigDecimal(m.group(3));

                TaxLine tax = new TaxLine(taxName, rate, amount);
                taxes.add(tax);
            } catch (NumberFormatException e) {
                // Skip malformed
            }
        }

        return taxes;
    }

    private BigDecimal extractTotal(String text) {
        // Look for "Total" keyword first (most reliable)
        Pattern totalPattern = Pattern.compile(
                "(?:^|\\n)\\s*(?:Total|Grand Total|Amount Due)\\s*[:\\$]?\\s*\\$?([0-9.]+)",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher m = totalPattern.matcher(text);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1));
            } catch (NumberFormatException e) {
                // Fall through
            }
        }

        // Fallback: find the largest currency amount in the text
        Pattern amountPattern = Pattern.compile("\\$([0-9.]+)");
        Matcher amountMatcher = amountPattern.matcher(text);
        BigDecimal max = BigDecimal.ZERO;
        while (amountMatcher.find()) {
            try {
                BigDecimal val = new BigDecimal(amountMatcher.group(1));
                if (val.compareTo(max) > 0) {
                    max = val;
                }
            } catch (NumberFormatException e) {
                // Skip
            }
        }
        return max.compareTo(BigDecimal.ZERO) > 0 ? max : BigDecimal.ZERO;
    }

    private List<LineItem> extractLineItems(String text) {
        List<LineItem> items = new ArrayList<>();

        // Pattern: description followed by amount on the same line
        // Examples: "Milk 2L                 $3.99" or "Item Name   $10.00"
        Pattern itemPattern = Pattern.compile("^\\s*(.+?)\\s{2,}\\$?([0-9.]+)\\s*$", Pattern.MULTILINE);
        Matcher m = itemPattern.matcher(text);

        Set<String> excludeKeywords = new HashSet<>(Arrays.asList(
                "tax", "vat", "gst", "total", "subtotal", "date", "time", "item", "price",
                "amount", "due", "payment", "charge", "fee", "discount", "sales", "grand"
        ));

        while (m.find()) {
            try {
                String description = m.group(1).trim();
                BigDecimal amount = new BigDecimal(m.group(2));

                // Filter out tax/total/header lines
                String descLower = description.toLowerCase();
                boolean isRelevant = true;

                for (String keyword : excludeKeywords) {
                    if (descLower.equals(keyword) || descLower.startsWith(keyword + " ") || descLower.endsWith(" " + keyword)) {
                        isRelevant = false;
                        break;
                    }
                }

                if (isRelevant && description.length() > 2 && amount.compareTo(BigDecimal.ZERO) > 0) {
                    items.add(new LineItem(description, amount));
                }
            } catch (NumberFormatException e) {
                // Skip malformed
            }
        }

        return items;
    }

    /**
     * Internal DTO for parsed receipt data.
     */
    private static class ParsedReceipt {
        String merchant;
        LocalDate date;
        String currency;
        BigDecimal total;
        List<TaxLine> taxes = new ArrayList<>();
        List<LineItem> items = new ArrayList<>();
    }
}
