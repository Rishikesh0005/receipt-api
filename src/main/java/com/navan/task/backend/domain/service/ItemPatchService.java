package com.navan.task.backend.domain.service;

import com.navan.task.backend.domain.exception.ReconciliationException;
import com.navan.task.backend.domain.exception.TransactionNotFoundException;
import com.navan.task.backend.domain.model.LineItem;
import com.navan.task.backend.domain.model.Transaction;
import com.navan.task.backend.domain.port.TransactionRepository;

import java.util.List;

/**
 * Orchestrates PATCH /transactions/{id}/items: applies the user's
 * edit/merge/split of line items. If the new set no longer reconciles
 * with the grand total and taxes, the whole update is rejected with the
 * mismatch details - we never silently adjust the total or items to
 * force a match.
 */
public class ItemPatchService {

    private final TransactionRepository transactionRepository;
    private final ReconciliationPolicy reconciliationPolicy;

    public ItemPatchService(TransactionRepository transactionRepository,
                             ReconciliationPolicy reconciliationPolicy) {
        this.transactionRepository = transactionRepository;
        this.reconciliationPolicy = reconciliationPolicy;
    }

    public Transaction patchItems(String transactionId, List<LineItem> newItems) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        ReconciliationPolicy.Result result =
                reconciliationPolicy.check(transaction.getTotal(), newItems, transaction.getTaxes());

        if (!result.isReconciled()) {
            throw new ReconciliationException(result);
        }

        transaction.setItems(newItems);
        transaction.setItemizeStatus(Transaction.ItemizeStatus.COMPLETE);

        return transactionRepository.save(transaction);
    }
}
