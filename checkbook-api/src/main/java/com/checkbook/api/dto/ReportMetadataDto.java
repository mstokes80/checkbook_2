package com.checkbook.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportMetadataDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Long> accountIds;
    private List<String> accountNames;
    private LocalDateTime generatedAt;

    public ReportMetadataDto() {
    }

    public ReportMetadataDto(LocalDate startDate, LocalDate endDate, List<Long> accountIds,
                            List<String> accountNames, LocalDateTime generatedAt) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.accountIds = accountIds;
        this.accountNames = accountNames;
        this.generatedAt = generatedAt;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<Long> getAccountIds() {
        return accountIds;
    }

    public void setAccountIds(List<Long> accountIds) {
        this.accountIds = accountIds;
    }

    public List<String> getAccountNames() {
        return accountNames;
    }

    public void setAccountNames(List<String> accountNames) {
        this.accountNames = accountNames;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}