package com.checkbook.api.repository;

import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.Transaction;
import com.checkbook.api.entity.TransactionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a specific account ordered by date descending, then by ID descending
     */
    Page<Transaction> findByAccountOrderByTransactionDateDescIdDesc(Account account, Pageable pageable);

    /**
     * Find all transactions for a specific account ID ordered by date descending, then by ID descending
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findByAccountIdOrderByTransactionDateDescIdDesc(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Find transactions by account and date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findByAccountIdAndTransactionDateBetween(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find transactions by account and category
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.category.id = :categoryId ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findByAccountIdAndCategoryId(@Param("accountId") Long accountId, @Param("categoryId") Long categoryId, Pageable pageable);

    /**
     * Find transactions by account, category, and date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.category.id = :categoryId AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findByAccountIdAndCategoryIdAndTransactionDateBetween(@Param("accountId") Long accountId, @Param("categoryId") Long categoryId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * Find transactions by account and amount range
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.amount BETWEEN :minAmount AND :maxAmount ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findByAccountIdAndAmountBetween(@Param("accountId") Long accountId, @Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount, Pageable pageable);

    /**
     * Search transactions by description (case-insensitive)
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findByAccountIdAndDescriptionContainingIgnoreCase(@Param("accountId") Long accountId, @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find credit transactions (positive amounts) for an account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.amount > 0 ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findCreditsByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Find debit transactions (negative amounts) for an account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.amount < 0 ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findDebitsByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Find recent transactions for an account (last N transactions)
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findRecentByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Find transaction by ID and account ID (for security)
     */
    @Query("SELECT t FROM Transaction t WHERE t.id = :transactionId AND t.account.id = :accountId")
    Optional<Transaction> findByIdAndAccountId(@Param("transactionId") Long transactionId, @Param("accountId") Long accountId);

    /**
     * Count transactions for an account
     */
    long countByAccount(Account account);

    /**
     * Count transactions for an account ID
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);

    /**
     * Count transactions in date range for an account
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId AND t.transactionDate BETWEEN :startDate AND :endDate")
    long countByAccountIdAndDateRange(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get sum of transaction amounts for an account in date range
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByAccountIdAndDateRange(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get sum of credit transactions for an account in date range
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.amount > 0 AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumCreditsInDateRange(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get sum of debit transactions for an account in date range
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.account.id = :accountId AND t.amount < 0 AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumDebitsInDateRange(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find transactions by category across all accounts
     */
    Page<Transaction> findByCategoryOrderByTransactionDateDescIdDesc(TransactionCategory category, Pageable pageable);

    /**
     * Find uncategorized transactions for an account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId AND t.category IS NULL ORDER BY t.transactionDate DESC, t.id DESC")
    Page<Transaction> findUncategorizedByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Update transaction running balance
     */
    @Modifying
    @Query("UPDATE Transaction t SET t.runningBalance = :runningBalance WHERE t.id = :transactionId")
    void updateRunningBalance(@Param("transactionId") Long transactionId, @Param("runningBalance") BigDecimal runningBalance);

    /**
     * Delete transactions older than specified date
     */
    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.createdAt < :cutoffDate")
    void deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find transactions with running balance recalculation needed (for data integrity checks)
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.transactionDate ASC, t.id ASC")
    List<Transaction> findByAccountIdOrderByTransactionDateAscIdAsc(@Param("accountId") Long accountId);

    /**
     * Get the latest transaction for an account (highest date and ID)
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findLatestByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Get transaction count by category
     */
    long countByCategory(TransactionCategory category);

    /**
     * Find transactions created in date range
     */
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    Page<Transaction> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    /**
     * Get average transaction amount for an account in date range
     */
    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.account.id = :accountId AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getAverageAmountByAccountIdAndDateRange(@Param("accountId") Long accountId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find top N largest transactions for an account
     */
    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId ORDER BY ABS(t.amount) DESC")
    List<Transaction> findTopTransactionsByAmountMagnitude(@Param("accountId") Long accountId, Pageable pageable);

    /**
     * Check if account has any transactions
     */
    boolean existsByAccount(Account account);

    /**
     * Check if account has any transactions by account ID
     */
    @Query("SELECT COUNT(t) > 0 FROM Transaction t WHERE t.account.id = :accountId")
    boolean existsByAccountId(@Param("accountId") Long accountId);

    /**
     * Find transactions for multiple accounts in a date range (for reporting)
     * Uses LEFT JOIN FETCH to eagerly load categories and accounts to avoid lazy initialization issues
     */
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category LEFT JOIN FETCH t.account WHERE t.account.id IN :accountIds AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findByAccountIdInAndTransactionDateBetween(@Param("accountIds") List<Long> accountIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}