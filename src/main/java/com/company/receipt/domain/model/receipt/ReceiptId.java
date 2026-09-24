package com.company.receipt.domain.model.receipt;

import java.util.UUID;
import java.util.Objects;

/**
 * Value object for Receipt ID.
 * Ensures type safety and prevents ID confusion.
 */
public class ReceiptId {
    private final String value;

    private ReceiptId(String value) {
        this.value = Objects.requireNonNull(value, "ReceiptId cannot be null");
    }

    public static ReceiptId generate() {
        return new ReceiptId(UUID.randomUUID().toString());
    }

    public static ReceiptId of(String value) {
        return new ReceiptId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReceiptId receiptId = (ReceiptId) o;
        return value.equals(receiptId.value);
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
