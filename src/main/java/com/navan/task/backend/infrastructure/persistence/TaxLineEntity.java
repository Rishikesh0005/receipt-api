package com.navan.task.backend.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * JPA entity for tax lines.
 */
@Entity
@Table(name = "tax_lines")
public class TaxLineEntity {
    @Id
    private String id;

    @Column
    private String name;

    @Column
    private BigDecimal rate;

    @Column
    private BigDecimal amount;

    @Column
    private String jurisdiction;

    @Column(name = "transaction_id")
    private String transactionId;

    public TaxLineEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
