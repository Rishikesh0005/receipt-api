package com.company.receipt.domain.model.transaction;

/**
 * Represents itemization status of a transaction.
 */
public enum ItemizeStatus {
    COMPLETE,      // Items reconcile perfectly with total and taxes
    NEEDS_REVIEW,  // Items extracted but don't reconcile
    FAILED         // No useful items extracted
}
