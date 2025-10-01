package com.checkbook.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private Account account;

    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Description is required")
    @Size(max = 255, message = "Description must be less than 255 characters")
    private String description;

    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TransactionCategory category;

    @Column(columnDefinition = "TEXT")
    @Size(max = 1000, message = "Notes must be less than 1000 characters")
    private String notes;

    @Column(name = "running_balance", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Running balance is required")
    private BigDecimal runningBalance;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Constructors
    public Transaction() {}

    public Transaction(Account account, BigDecimal amount, String description, LocalDate transactionDate) {
        this.account = account;
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.runningBalance = BigDecimal.ZERO; // Will be calculated by database triggers
    }

    public Transaction(Account account, BigDecimal amount, String description, LocalDate transactionDate, TransactionCategory category, String notes) {
        this(account, amount, description, transactionDate);
        this.category = category;
        this.notes = notes;
    }

    // Business methods
    public boolean isCredit() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isDebit() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public BigDecimal getAbsoluteAmount() {
        return amount.abs();
    }

    public String getTransactionTypeDisplay() {
        return isCredit() ? "Credit" : "Debit";
    }

    public String getFormattedAmount() {
        if (isCredit()) {
            return "+" + amount.toString();
        }
        return amount.toString();
    }

    public boolean belongsToAccount(Account account) {
        return this.account != null && this.account.getId().equals(account.getId());
    }

    public boolean isOnDate(LocalDate date) {
        return this.transactionDate.equals(date);
    }

    public boolean isInDateRange(LocalDate startDate, LocalDate endDate) {
        return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                ", runningBalance=" + runningBalance +
                '}';
    }
}