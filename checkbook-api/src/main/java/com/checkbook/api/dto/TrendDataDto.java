package com.checkbook.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TrendDataDto {
    private LocalDate date;
    private BigDecimal totalExpenses;
    private BigDecimal totalIncome;

    public TrendDataDto() {
    }

    public TrendDataDto(LocalDate date, BigDecimal totalExpenses, BigDecimal totalIncome) {
        this.date = date;
        this.totalExpenses = totalExpenses;
        this.totalIncome = totalIncome;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }
}