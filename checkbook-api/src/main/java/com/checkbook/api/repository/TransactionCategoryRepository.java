package com.checkbook.api.repository;

import com.checkbook.api.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {

    /**
     * Find all categories ordered by name
     */
    List<TransactionCategory> findAllByOrderByNameAsc();

    /**
     * Find all system default categories
     */
    List<TransactionCategory> findByIsSystemDefaultTrueOrderByNameAsc();

    /**
     * Find all user-created categories (non-system)
     */
    List<TransactionCategory> findByIsSystemDefaultFalseOrderByNameAsc();

    /**
     * Find category by name (case-insensitive)
     */
    @Query("SELECT tc FROM TransactionCategory tc WHERE LOWER(tc.name) = LOWER(:name)")
    Optional<TransactionCategory> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Check if category name exists (case-insensitive)
     */
    @Query("SELECT COUNT(tc) > 0 FROM TransactionCategory tc WHERE LOWER(tc.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    /**
     * Check if category name exists excluding current category (for updates)
     */
    @Query("SELECT COUNT(tc) > 0 FROM TransactionCategory tc WHERE LOWER(tc.name) = LOWER(:name) AND tc.id != :categoryId")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("categoryId") Long categoryId);

    /**
     * Find categories by name containing text (case-insensitive)
     */
    @Query("SELECT tc FROM TransactionCategory tc WHERE LOWER(tc.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY tc.name ASC")
    List<TransactionCategory> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Find system default category by name
     */
    @Query("SELECT tc FROM TransactionCategory tc WHERE tc.isSystemDefault = true AND LOWER(tc.name) = LOWER(:name)")
    Optional<TransactionCategory> findSystemCategoryByName(@Param("name") String name);

    /**
     * Count system default categories
     */
    long countByIsSystemDefaultTrue();

    /**
     * Count user-created categories
     */
    long countByIsSystemDefaultFalse();

    /**
     * Find categories with transaction count
     */
    @Query("SELECT tc, COUNT(t) as transactionCount FROM TransactionCategory tc LEFT JOIN tc.transactions t GROUP BY tc ORDER BY tc.name ASC")
    List<Object[]> findCategoriesWithTransactionCount();

    /**
     * Find categories that have transactions
     */
    @Query("SELECT DISTINCT tc FROM TransactionCategory tc INNER JOIN tc.transactions t ORDER BY tc.name ASC")
    List<TransactionCategory> findCategoriesWithTransactions();

    /**
     * Find categories that have no transactions
     */
    @Query("SELECT tc FROM TransactionCategory tc WHERE tc.id NOT IN (SELECT DISTINCT t.category.id FROM Transaction t WHERE t.category IS NOT NULL) ORDER BY tc.name ASC")
    List<TransactionCategory> findCategoriesWithoutTransactions();

    /**
     * Find user-created categories that can be deleted (no transactions)
     */
    @Query("SELECT tc FROM TransactionCategory tc WHERE tc.isSystemDefault = false AND tc.id NOT IN (SELECT DISTINCT t.category.id FROM Transaction t WHERE t.category IS NOT NULL) ORDER BY tc.name ASC")
    List<TransactionCategory> findDeletableUserCategories();

    /**
     * Get default "Uncategorized" category
     */
    @Query("SELECT tc FROM TransactionCategory tc WHERE tc.isSystemDefault = true AND LOWER(tc.name) = 'uncategorized'")
    Optional<TransactionCategory> findUncategorizedCategory();

    /**
     * Get most frequently used categories
     */
    @Query("SELECT tc, COUNT(t) as usageCount FROM TransactionCategory tc INNER JOIN tc.transactions t GROUP BY tc ORDER BY COUNT(t) DESC, tc.name ASC")
    List<Object[]> findMostUsedCategories();

    /**
     * Find categories created in the last N days using LocalDateTime calculation
     */
    @Query("SELECT tc FROM TransactionCategory tc WHERE tc.isSystemDefault = false AND tc.createdAt >= :dateThreshold ORDER BY tc.createdAt DESC")
    List<TransactionCategory> findRecentUserCategories(@Param("dateThreshold") java.time.LocalDateTime dateThreshold);

    /**
     * Get transaction count for a specific category
     */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.category.id = :categoryId")
    long getTransactionCountByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Check if category can be safely deleted (is not system and has no transactions)
     */
    @Query("SELECT COUNT(t) = 0 AND tc.isSystemDefault = false FROM TransactionCategory tc LEFT JOIN tc.transactions t WHERE tc.id = :categoryId GROUP BY tc.id, tc.isSystemDefault")
    Boolean canCategoryBeDeleted(@Param("categoryId") Long categoryId);

    /**
     * Find all categories with their transaction counts for reporting
     */
    @Query("SELECT new map(tc.id as id, tc.name as name, tc.isSystemDefault as isSystem, COUNT(t) as transactionCount) " +
           "FROM TransactionCategory tc LEFT JOIN tc.transactions t " +
           "GROUP BY tc.id, tc.name, tc.isSystemDefault " +
           "ORDER BY tc.name ASC")
    List<java.util.Map<String, Object>> findCategorySummary();
}