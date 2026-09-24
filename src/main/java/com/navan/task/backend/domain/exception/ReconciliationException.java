package com.navan.task.backend.domain.exception;

import com.navan.task.backend.domain.service.ReconciliationPolicy;

/**
 * Thrown when a user's item edits no longer reconcile with the
 * transaction's grand total and taxes. Carries the mismatch details so
 * the web layer can build a useful 409 response - the update itself is
 * always rejected outright, never partially applied.
 */
public class ReconciliationException extends RuntimeException {

    private final ReconciliationPolicy.Result result;

    public ReconciliationException(ReconciliationPolicy.Result result) {
        super("Line items and taxes do not reconcile with the transaction total");
        this.result = result;
    }

    public ReconciliationPolicy.Result getResult() {
        return result;
    }
}
