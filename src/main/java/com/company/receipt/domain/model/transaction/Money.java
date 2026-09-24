package com.company.receipt.domain.model.transaction;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object for Money.
 * Ensures BigDecimal consistency and prevents floating point errors.
 */
public class Money {
    private static final int SCALE = 2;
    
    private final BigDecimal amount;
    private final String currency;

    private Money(BigDecimal amount, String currency) {
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null")
                .setScale(SCALE, java.math.RoundingMode.HALF_UP);
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public BigDecimal amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public Money plus(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money minus(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean equals(Money other) {
        return this.amount.compareTo(other.amount) == 0 && this.currency.equals(other.currency);
    }

    @Override
    public String toString() {
        return currency + " " + amount;
    }
}
