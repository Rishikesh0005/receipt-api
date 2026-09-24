package com.navan.task.backend.domain.exception;

/**
 * Thrown when a transaction id does not exist.
 */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String id) {
        super("Transaction not found: " + id);
    }
}
