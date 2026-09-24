package com.navan.task.backend.application.dto;

import com.navan.task.backend.domain.service.ReconciliationPolicy;

import java.math.BigDecimal;

/**
 * 409 response body for a reconciliation mismatch, carrying the expected
 * total, the actual sum of items + taxes, and the difference between them.
 */
public class ReconciliationErrorDto {
    public String code;
    public String message;
    public BigDecimal expectedTotal;
    public BigDecimal actualSum;
    public BigDecimal difference;

    public ReconciliationErrorDto() {
    }

    public ReconciliationErrorDto(String code, String message, BigDecimal expectedTotal,
                                   BigDecimal actualSum, BigDecimal difference) {
        this.code = code;
        this.message = message;
        this.expectedTotal = expectedTotal;
        this.actualSum = actualSum;
        this.difference = difference;
    }

    public static ReconciliationErrorDto from(ReconciliationPolicy.Result result) {
        return new ReconciliationErrorDto(
                "RECONCILIATION_MISMATCH",
                "Line items and taxes do not reconcile with the transaction total",
                result.getExpectedTotal(),
                result.getActualSum(),
                result.getDelta()
        );
    }
}
