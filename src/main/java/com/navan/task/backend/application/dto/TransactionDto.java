package com.navan.task.backend.application.dto;

import com.navan.task.backend.domain.model.LineItem;
import com.navan.task.backend.domain.model.TaxLine;
import com.navan.task.backend.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response shape for a transaction, including its taxes and line items.
 */
public class TransactionDto {
    public String id;
    public String receiptId;
    public String merchant;
    public LocalDate date;
    public String currency;
    public BigDecimal total;
    public String itemizeStatus;
    public List<TaxLineDto> taxes;
    public List<LineItemDto> items;

    public TransactionDto() {
    }

    public TransactionDto(String id, String receiptId, String merchant, LocalDate date,
                          String currency, BigDecimal total, String itemizeStatus,
                          List<TaxLineDto> taxes, List<LineItemDto> items) {
        this.id = id;
        this.receiptId = receiptId;
        this.merchant = merchant;
        this.date = date;
        this.currency = currency;
        this.total = total;
        this.itemizeStatus = itemizeStatus;
        this.taxes = taxes;
        this.items = items;
    }

    /**
     * Build the response shape for a domain Transaction. Kept here (rather
     * than in the controller) so response shaping stays a DTO concern.
     */
    public static TransactionDto from(Transaction transaction) {
        List<TaxLineDto> taxDtos = transaction.getTaxes().stream()
                .map(TaxLineDto::from)
                .collect(Collectors.toList());

        List<LineItemDto> itemDtos = transaction.getItems().stream()
                .map(LineItemDto::from)
                .collect(Collectors.toList());

        return new TransactionDto(
                transaction.getId(),
                transaction.getReceiptId(),
                transaction.getMerchant(),
                transaction.getDate(),
                transaction.getCurrency(),
                transaction.getTotal(),
                transaction.getItemizeStatus().toString(),
                taxDtos,
                itemDtos
        );
    }

    public static class TaxLineDto {
        public String id;
        public String name;
        public BigDecimal rate;
        public BigDecimal amount;
        public String jurisdiction;

        public TaxLineDto() {
        }

        public TaxLineDto(String id, String name, BigDecimal rate, BigDecimal amount, String jurisdiction) {
            this.id = id;
            this.name = name;
            this.rate = rate;
            this.amount = amount;
            this.jurisdiction = jurisdiction;
        }

        public static TaxLineDto from(TaxLine tax) {
            return new TaxLineDto(tax.getId(), tax.getName(), tax.getRate(), tax.getAmount(), tax.getJurisdiction());
        }
    }

    public static class LineItemDto {
        public String id;
        public String description;
        public BigDecimal amount;
        public BigDecimal quantity;
        public BigDecimal taxAmount;

        public LineItemDto() {
        }

        public LineItemDto(String id, String description, BigDecimal amount, BigDecimal quantity, BigDecimal taxAmount) {
            this.id = id;
            this.description = description;
            this.amount = amount;
            this.quantity = quantity;
            this.taxAmount = taxAmount;
        }

        public static LineItemDto from(LineItem item) {
            return new LineItemDto(item.getId(), item.getDescription(), item.getAmount(), item.getQuantity(), item.getTaxAmount());
        }
    }
}

