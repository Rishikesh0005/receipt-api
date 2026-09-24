package com.navan.task.backend.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Core domain model for tax line items.
 */
public class TaxLine {
    private final String id;
    private String name;
    private BigDecimal rate;
    private BigDecimal amount;
    private String jurisdiction;

    public TaxLine(String name, BigDecimal rate, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.rate = rate;
        this.amount = amount;
    }

    private TaxLine(String id, String name, BigDecimal rate, BigDecimal amount) {
        this.id = id;
        this.name = name;
        this.rate = rate;
        this.amount = amount;
    }

    /**
     * Rebuild a TaxLine from persisted state, preserving its original id.
     */
    public static TaxLine restore(String id, String name, BigDecimal rate, BigDecimal amount) {
        return new TaxLine(id, name, rate, amount);
    }

    public String getId() {
        return id;
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
}
