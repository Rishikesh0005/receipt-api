package com.company.receipt.domain.exception;

/**
 * Thrown when itemization fails to reconcile with transaction total.
 */
public class ItemizationMismatchException extends RuntimeException {
    private final java.math.BigDecimal expected;
    private final java.math.BigDecimal actual;

    public ItemizationMismatchException(String message, java.math.BigDecimal expected, java.math.BigDecimal actual) {
        super(message);
        this.expected = expected;
        this.actual = actual;
    }

    public java.math.BigDecimal getExpected() {
        return expected;
    }

    public java.math.BigDecimal getActual() {
        return actual;
    }

    public java.math.BigDecimal getDifference() {
        return expected.subtract(actual);
    }
}
