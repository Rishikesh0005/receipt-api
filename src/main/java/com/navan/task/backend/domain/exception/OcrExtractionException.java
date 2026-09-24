package com.navan.task.backend.domain.exception;

/**
 * Thrown when OCR text extraction fails or returns nothing usable.
 */
public class OcrExtractionException extends RuntimeException {
    public OcrExtractionException(String message) {
        super(message);
    }
}
