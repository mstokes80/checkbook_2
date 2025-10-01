package com.checkbook.api.dto;

import java.math.BigDecimal;

public class CategoryBreakdownDto {
    private Long categoryId;
    private String categoryName;
    private BigDecimal totalAmount;
    private Integer transactionCount;
    private Double percentage;

    public CategoryBreakdownDto() {
    }

    public CategoryBreakdownDto(Long categoryId, String categoryName, BigDecimal totalAmount,
                               Integer transactionCount, Double percentage) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
        this.percentage = percentage;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }
}