package com.navan.task.backend.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core domain model for a transaction extracted from a receipt.
 */
public class Transaction {
    public enum ItemizeStatus {
        COMPLETE, NEEDS_REVIEW, FAILED
    }

    private final String id;
    private final String receiptId;
    private String merchant;
    private LocalDate date;
    private String currency;
    private BigDecimal total;
    private ItemizeStatus itemizeStatus;
    private List<TaxLine> taxes;
    private List<LineItem> items;

    public Transaction(String receiptId) {
        this.id = UUID.randomUUID().toString();
        this.receiptId = receiptId;
        this.taxes = new ArrayList<>();
        this.items = new ArrayList<>();
        this.itemizeStatus = ItemizeStatus.FAILED;
    }

    private Transaction(String id, String receiptId) {
        this.id = id;
        this.receiptId = receiptId;
        this.taxes = new ArrayList<>();
        this.items = new ArrayList<>();
        this.itemizeStatus = ItemizeStatus.FAILED;
    }

    /**
     * Rebuild a Transaction from persisted state, preserving its original id.
     * Use this in repository adapters instead of the public constructor,
     * which always mints a brand-new id.
     */
    public static Transaction restore(String id, String receiptId) {
        return new Transaction(id, receiptId);
    }

    public String getId() {
        return id;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public ItemizeStatus getItemizeStatus() {
        return itemizeStatus;
    }

    public void setItemizeStatus(ItemizeStatus itemizeStatus) {
        this.itemizeStatus = itemizeStatus;
    }

    public List<TaxLine> getTaxes() {
        return taxes;
    }

    public void setTaxes(List<TaxLine> taxes) {
        this.taxes = taxes;
    }

    public List<LineItem> getItems() {
        return items;
    }

    public void setItems(List<LineItem> items) {
        this.items = items;
    }
}
