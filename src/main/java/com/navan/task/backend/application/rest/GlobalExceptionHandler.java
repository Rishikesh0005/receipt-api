package com.navan.task.backend.application.rest;

import com.navan.task.backend.application.dto.ErrorResponseDto;
import com.navan.task.backend.application.dto.ReconciliationErrorDto;
import com.navan.task.backend.domain.exception.EmptyFileException;
import com.navan.task.backend.domain.exception.OcrExtractionException;
import com.navan.task.backend.domain.exception.ReceiptNotFoundException;
import com.navan.task.backend.domain.exception.ReconciliationException;
import com.navan.task.backend.domain.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain exceptions into HTTP responses. Controllers never
 * need try/catch blocks - they let domain exceptions propagate here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReconciliationException.class)
    public ResponseEntity<ReconciliationErrorDto> handleReconciliation(ReconciliationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ReconciliationErrorDto.from(ex.getResult()));
    }

    @ExceptionHandler({TransactionNotFoundException.class, ReceiptNotFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ErrorResponseDto> handleEmptyFile(EmptyFileException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto("INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(OcrExtractionException.class)
    public ResponseEntity<ErrorResponseDto> handleOcrFailure(OcrExtractionException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto("OCR_FAILED", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDto("INTERNAL_ERROR", ex.getMessage()));
    }
}
