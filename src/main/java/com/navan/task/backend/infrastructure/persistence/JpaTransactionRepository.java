package com.navan.task.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Spring Data JPA repository for transactions.
 */
public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, String> {
    Optional<TransactionEntity> findByReceiptId(String receiptId);
}
