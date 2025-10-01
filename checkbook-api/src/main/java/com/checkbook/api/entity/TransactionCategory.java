package com.checkbook.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transaction_categories")
@EntityListeners(AuditingEntityListener.class)
public class TransactionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be less than 100 characters")
    private String name;

    @Column(name = "is_system_default", nullable = false)
    @NotNull(message = "System default status is required")
    private Boolean isSystemDefault = false;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // Constructors
    public TransactionCategory() {}

    public TransactionCategory(String name) {
        this.name = name;
        this.isSystemDefault = false;
    }

    public TransactionCategory(String name, Boolean isSystemDefault) {
        this.name = name;
        this.isSystemDefault = isSystemDefault;
    }

    // Business methods
    public boolean isSystemCategory() {
        return isSystemDefault != null && isSystemDefault;
    }

    public boolean canBeDeleted() {
        // System default categories cannot be deleted
        return !isSystemCategory();
    }

    public int getTransactionCount() {
        return transactions != null ? transactions.size() : 0;
    }

    public boolean hasTransactions() {
        return getTransactionCount() > 0;
    }

    public String getDisplayName() {
        if (isSystemCategory()) {
            return name + " (System)";
        }
        return name;
    }

    // Static methods for common categories
    public static TransactionCategory createSystemCategory(String name) {
        return new TransactionCategory(name, true);
    }

    public static TransactionCategory createUserCategory(String name) {
        return new TransactionCategory(name, false);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsSystemDefault() {
        return isSystemDefault;
    }

    public void setIsSystemDefault(Boolean isSystemDefault) {
        this.isSystemDefault = isSystemDefault;
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

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @Override
    public String toString() {
        return "TransactionCategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isSystemDefault=" + isSystemDefault +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionCategory that = (TransactionCategory) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}