package com.navan.task.backend.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * JPA entity for storing transactions extracted from receipts.
 */
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String receiptId;

    @Column
    private String merchant;

    @Column
    private LocalDate date;

    @Column
    private String currency;

    @Column
    private BigDecimal total;

    @Column
    @Enumerated(EnumType.STRING)
    private ItemizeStatus itemizeStatus;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "transaction_id")
    private List<TaxLineEntity> taxes;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "transaction_id")
    private List<LineItemEntity> items;

    public enum ItemizeStatus {
        COMPLETE, NEEDS_REVIEW, FAILED
    }

    public TransactionEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public ItemizeStatus getItemizeStatus() {
        return itemizeStatus;
    }

    public void setItemizeStatus(ItemizeStatus itemizeStatus) {
        this.itemizeStatus = itemizeStatus;
    }

    public List<TaxLineEntity> getTaxes() {
        return taxes;
    }

    public void setTaxes(List<TaxLineEntity> taxes) {
        this.taxes = taxes;
    }

    public List<LineItemEntity> getItems() {
        return items;
    }

    public void setItems(List<LineItemEntity> items) {
        this.items = items;
    }
}
