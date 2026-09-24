package com.navan.task.backend.domain.port;

import com.navan.task.backend.domain.model.Transaction;
import java.util.List;
import java.util.Optional;

/**
 * Output port: Transaction persistence contract.
 */
public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(String id);
    Optional<Transaction> findByReceiptId(String receiptId);

    /** Most recently created first. */
    List<Transaction> findAll();
}
