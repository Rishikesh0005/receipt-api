package com.navan.task.backend.domain.exception;

/**
 * Thrown when a receipt id does not exist.
 */
public class ReceiptNotFoundException extends RuntimeException {
    public ReceiptNotFoundException(String id) {
        super("Receipt not found: " + id);
    }
}
