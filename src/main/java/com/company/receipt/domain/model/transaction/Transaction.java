package com.company.receipt.domain.model.transaction;

import com.company.receipt.domain.model.receipt.ReceiptId;
import com.company.receipt.domain.model.tax.Tax;
import com.company.receipt.domain.model.item.LineItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Transaction aggregate root.
 * Represents a parsed receipt with extracted transaction data.
 */
public class Transaction {
    private final TransactionId id;
    private final ReceiptId receiptId;
    private String merchant;
    private LocalDate date;
    private String currency;
    private BigDecimal total;
    private ItemizeStatus itemizeStatus;
    private List<Tax> taxes;
    private List<LineItem> items;

    private Transaction(TransactionId id, ReceiptId receiptId) {
        this.id = Objects.requireNonNull(id);
        this.receiptId = Objects.requireNonNull(receiptId);
        this.taxes = new ArrayList<>();
        this.items = new ArrayList<>();
        this.itemizeStatus = ItemizeStatus.FAILED;
    }

    public static Transaction create(ReceiptId receiptId) {
        return new Transaction(TransactionId.generate(), receiptId);
    }

    public static Transaction restore(TransactionId id, ReceiptId receiptId) {
        return new Transaction(id, receiptId);
    }

    // Getters
    public TransactionId getId() {
        return id;
    }

    public ReceiptId getReceiptId() {
        return receiptId;
    }

    public String getMerchant() {
        return merchant;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public ItemizeStatus getItemizeStatus() {
        return itemizeStatus;
    }

    public List<Tax> getTaxes() {
        return new ArrayList<>(taxes);
    }

    public List<LineItem> getItems() {
        return new ArrayList<>(items);
    }

    // Setters (used during processing)
    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void setTaxes(List<Tax> taxes) {
        this.taxes = new ArrayList<>(taxes);
    }

    public void setItems(List<LineItem> items) {
        this.items = new ArrayList<>(items);
    }

    public void setItemizeStatus(ItemizeStatus itemizeStatus) {
        this.itemizeStatus = itemizeStatus;
    }

    // Business methods
    public BigDecimal calculateItemTotal() {
        return items.stream()
                .map(LineItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTaxTotal() {
        return taxes.stream()
                .map(Tax::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean reconciles() {
        BigDecimal itemSum = calculateItemTotal();
        BigDecimal taxSum = calculateTaxTotal();
        BigDecimal calculated = itemSum.add(taxSum);
        return calculated.compareTo(total) == 0;
    }

    public void replaceItems(List<LineItem> newItems) {
        this.items = new ArrayList<>(newItems);
    }
}
