package com.checkbook.api.service;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.Transaction;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.AccountRepository;
import com.checkbook.api.repository.AccountPermissionRepository;
import com.checkbook.api.repository.TransactionRepository;
import com.checkbook.api.security.AccountSecurityEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountPermissionRepository accountPermissionRepository;

    @Autowired
    private AccountSecurityEvaluator accountSecurityEvaluator;

    @Transactional(readOnly = true)
    public SpendingReportResponseDto generateSpendingReport(
            User user, LocalDate startDate, LocalDate endDate, List<Long> accountIds) {

        // Validate date range
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // If no account IDs provided, get all accessible accounts
        List<Long> effectiveAccountIds = accountIds;
        if (effectiveAccountIds == null || effectiveAccountIds.isEmpty()) {
            effectiveAccountIds = accountRepository.findAll().stream()
                    .filter(account -> hasViewerPermissionOrHigher(user, account))
                    .map(Account::getId)
                    .collect(Collectors.toList());
        } else {
            // Validate user has permission for all requested accounts
            for (Long accountId : effectiveAccountIds) {
                Account account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
                if (!hasViewerPermissionOrHigher(user, account)) {
                    throw new SecurityException("No permission to view account: " + accountId);
                }
            }
        }

        // Get account names
        List<String> accountNames = accountRepository.findAllById(effectiveAccountIds).stream()
                .map(Account::getName)
                .collect(Collectors.toList());

        // Fetch all transactions for the date range and accounts
        List<Transaction> transactions = transactionRepository
                .findByAccountIdInAndTransactionDateBetween(
                        effectiveAccountIds, startDate, endDate);

        // Build report metadata
        ReportMetadataDto metadata = new ReportMetadataDto(
                startDate, endDate, effectiveAccountIds, accountNames, LocalDateTime.now());

        // Calculate summary
        ReportSummaryDto summary = calculateSummary(transactions);

        // Calculate category breakdown
        List<CategoryBreakdownDto> categoryBreakdown = calculateCategoryBreakdown(transactions);

        // Get top 5 categories
        List<CategoryBreakdownDto> topCategories = categoryBreakdown.stream()
                .limit(5)
                .collect(Collectors.toList());

        // Generate trend data
        List<TrendDataDto> trendData = generateTrendData(transactions, startDate, endDate);

        return new SpendingReportResponseDto(metadata, summary, categoryBreakdown, topCategories, trendData);
    }

    private ReportSummaryDto calculateSummary(List<Transaction> transactions) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                totalIncome = totalIncome.add(transaction.getAmount());
            } else {
                totalExpenses = totalExpenses.add(transaction.getAmount().abs());
            }
        }

        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        return new ReportSummaryDto(totalIncome, totalExpenses, netBalance, transactions.size());
    }

    private List<CategoryBreakdownDto> calculateCategoryBreakdown(List<Transaction> transactions) {
        // Group by category
        Map<Long, List<Transaction>> transactionsByCategory = transactions.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(t -> t.getCategory().getId()));

        // Calculate total expenses for percentage calculation (only negative amounts)
        BigDecimal totalExpenses = transactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownDto> breakdown = new ArrayList<>();

        for (Map.Entry<Long, List<Transaction>> entry : transactionsByCategory.entrySet()) {
            Long categoryId = entry.getKey();
            List<Transaction> categoryTransactions = entry.getValue();

            String categoryName = categoryTransactions.get(0).getCategory().getName();

            // Only count expenses for category breakdown
            BigDecimal categoryTotal = categoryTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(t -> t.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int transactionCount = (int) categoryTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .count();

            // Calculate percentage
            double percentage = 0.0;
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0 && categoryTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = categoryTotal.divide(totalExpenses, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .doubleValue();
            }

            if (categoryTotal.compareTo(BigDecimal.ZERO) > 0) {
                breakdown.add(new CategoryBreakdownDto(
                        categoryId, categoryName, categoryTotal, transactionCount, percentage));
            }
        }

        // Sort by total amount descending
        breakdown.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));

        return breakdown;
    }

    private List<TrendDataDto> generateTrendData(List<Transaction> transactions,
                                                  LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        boolean groupByDay = daysBetween <= 31;

        Map<LocalDate, List<Transaction>> groupedTransactions;

        if (groupByDay) {
            // Group by day
            groupedTransactions = transactions.stream()
                    .collect(Collectors.groupingBy(Transaction::getTransactionDate));
        } else {
            // Group by week (start of week)
            groupedTransactions = transactions.stream()
                    .collect(Collectors.groupingBy(t ->
                        t.getTransactionDate().minusDays(t.getTransactionDate().getDayOfWeek().getValue() - 1)));
        }

        List<TrendDataDto> trendData = new ArrayList<>();

        for (Map.Entry<LocalDate, List<Transaction>> entry : groupedTransactions.entrySet()) {
            LocalDate date = entry.getKey();
            List<Transaction> dayTransactions = entry.getValue();

            BigDecimal dayIncome = dayTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) > 0)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal dayExpenses = dayTransactions.stream()
                    .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .map(t -> t.getAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            trendData.add(new TrendDataDto(date, dayExpenses, dayIncome));
        }

        // Sort by date
        trendData.sort(Comparator.comparing(TrendDataDto::getDate));

        return trendData;
    }

    private boolean hasViewerPermissionOrHigher(User user, Account account) {
        // Check if user is the owner
        if (account.getOwner().getId().equals(user.getId())) {
            return true;
        }

        // Check if user has any permission on this account using the repository
        return accountPermissionRepository.existsByAccountIdAndUserId(account.getId(), user.getId());
    }
}