package com.company.receipt.domain.model.item;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simple line item entity.
 */
public class LineItem {
    private final String id;
    private String description;
    private BigDecimal amount;

    public LineItem(String description, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
