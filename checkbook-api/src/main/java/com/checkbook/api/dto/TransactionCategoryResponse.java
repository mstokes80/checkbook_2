package com.checkbook.api.dto;

import com.checkbook.api.entity.TransactionCategory;

import java.time.LocalDateTime;

public class TransactionCategoryResponse {

    private Long id;
    private String name;
    private Boolean isSystemDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int transactionCount;
    private String displayName;
    private boolean canBeDeleted;

    // Constructors
    public TransactionCategoryResponse() {}

    public TransactionCategoryResponse(TransactionCategory category) {
        this.id = category.getId();
        this.name = category.getName();
        this.isSystemDefault = category.getIsSystemDefault();
        this.createdAt = category.getCreatedAt();
        this.updatedAt = category.getUpdatedAt();
        this.transactionCount = category.getTransactionCount();
        this.displayName = category.getDisplayName();
        this.canBeDeleted = category.canBeDeleted();
    }

    public TransactionCategoryResponse(TransactionCategory category, int transactionCount) {
        this(category);
        this.transactionCount = transactionCount;
    }

    // Static factory methods
    public static TransactionCategoryResponse fromEntity(TransactionCategory category) {
        return new TransactionCategoryResponse(category);
    }

    public static TransactionCategoryResponse fromEntityWithCount(TransactionCategory category, int transactionCount) {
        return new TransactionCategoryResponse(category, transactionCount);
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

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isCanBeDeleted() {
        return canBeDeleted;
    }

    public void setCanBeDeleted(boolean canBeDeleted) {
        this.canBeDeleted = canBeDeleted;
    }

    @Override
    public String toString() {
        return "TransactionCategoryResponse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isSystemDefault=" + isSystemDefault +
                ", transactionCount=" + transactionCount +
                '}';
    }
}