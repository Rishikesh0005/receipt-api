package com.navan.task.backend.domain.exception;

/**
 * Thrown when an uploaded file is missing or empty.
 */
public class EmptyFileException extends RuntimeException {
    public EmptyFileException() {
        super("Uploaded file is empty");
    }
}
