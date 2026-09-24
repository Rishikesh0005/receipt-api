package com.navan.task.backend.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Core domain model for line items on a receipt.
 */
public class LineItem {
    private final String id;
    private String description;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal taxAmount;

    public LineItem(String description, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.amount = amount;
        this.quantity = BigDecimal.ONE;
    }

    private LineItem(String id, String description, BigDecimal amount) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.quantity = BigDecimal.ONE;
    }

    /**
     * Rebuild a LineItem from persisted state, preserving its original id.
     */
    public static LineItem restore(String id, String description, BigDecimal amount) {
        return new LineItem(id, description, amount);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
}
