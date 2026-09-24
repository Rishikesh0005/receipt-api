package com.company.receipt.domain.model.tax;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simple tax line entity.
 */
public class Tax {
    private final String id;
    private String name;
    private BigDecimal rate;
    private BigDecimal amount;

    public Tax(String name, BigDecimal rate, BigDecimal amount) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.rate = rate;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
