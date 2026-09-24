package com.navan.task.backend.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * JPA entity for line items.
 */
@Entity
@Table(name = "line_items")
public class LineItemEntity {
    @Id
    private String id;

    @Column
    private String description;

    @Column
    private BigDecimal amount;

    @Column
    private BigDecimal quantity;

    @Column
    private BigDecimal taxAmount;

    @Column(name = "transaction_id")
    private String transactionId;

    public LineItemEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
