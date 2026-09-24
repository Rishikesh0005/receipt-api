package com.navan.task.backend.domain.service;

import com.navan.task.backend.domain.model.LineItem;
import com.navan.task.backend.domain.model.TaxLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Single source of truth for "do the numbers add up".
 * <p>
 * Used identically when a receipt is first processed and when a user
 * PATCHes line items, so the two code paths can never silently drift
 * apart. A mismatch is always reported, never silently forced to match.
 */
public class ReconciliationPolicy {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

    public Result check(BigDecimal grandTotal, List<LineItem> items, List<TaxLine> taxes) {
        BigDecimal itemSum = sum(items, LineItem::getAmount);
        BigDecimal taxSum = sum(taxes, TaxLine::getAmount);

        BigDecimal actual = itemSum.add(taxSum).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expected = grandTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal delta = actual.subtract(expected).abs();

        boolean reconciled = !items.isEmpty() && delta.compareTo(TOLERANCE) <= 0;
        return new Result(reconciled, expected, actual, delta);
    }

    private <T> BigDecimal sum(List<T> values, java.util.function.Function<T, BigDecimal> extractor) {
        return values.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Outcome of a reconciliation check, carrying enough detail to build
     * a useful error response when the numbers don't match.
     */
    public static class Result {
        private final boolean reconciled;
        private final BigDecimal expectedTotal;
        private final BigDecimal actualSum;
        private final BigDecimal delta;

        public Result(boolean reconciled, BigDecimal expectedTotal, BigDecimal actualSum, BigDecimal delta) {
            this.reconciled = reconciled;
            this.expectedTotal = expectedTotal;
            this.actualSum = actualSum;
            this.delta = delta;
        }

        public boolean isReconciled() {
            return reconciled;
        }

        public BigDecimal getExpectedTotal() {
            return expectedTotal;
        }

        public BigDecimal getActualSum() {
            return actualSum;
        }

        public BigDecimal getDelta() {
            return delta;
        }
    }
}
