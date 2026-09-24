package com.navan.task.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for receipts.
 */
public interface JpaReceiptRepository extends JpaRepository<ReceiptEntity, String> {
}
