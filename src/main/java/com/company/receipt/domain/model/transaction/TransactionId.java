package com.company.receipt.domain.model.transaction;

import java.util.UUID;
import java.util.Objects;

/**
 * Value object for Transaction ID.
 * Ensures type safety and prevents ID confusion.
 */
public class TransactionId {
    private final String value;

    private TransactionId(String value) {
        this.value = Objects.requireNonNull(value, "TransactionId cannot be null");
    }

    public static TransactionId generate() {
        return new TransactionId(UUID.randomUUID().toString());
    }

    public static TransactionId of(String value) {
        return new TransactionId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
