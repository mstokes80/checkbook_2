package com.checkbook.api.repository;

import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.AccountType;
import com.checkbook.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find all accounts owned by a specific user
     */
    List<Account> findByOwnerOrderByCreatedAtDesc(User owner);

    /**
     * Find all accounts owned by a specific user ID
     */
    @Query("SELECT a FROM Account a WHERE a.owner.id = :ownerId ORDER BY a.createdAt DESC")
    List<Account> findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);

    /**
     * Find accounts that a user can access (owned + shared accounts with permissions)
     */
    @Query("SELECT DISTINCT a FROM Account a LEFT JOIN AccountPermission ap ON a.id = ap.account.id " +
           "WHERE a.owner.id = :userId OR (a.isShared = true AND ap.user.id = :userId)")
    List<Account> findAccessibleAccountsByUserId(@Param("userId") Long userId);

    /**
     * Find all shared accounts
     */
    List<Account> findByIsSharedTrueOrderByCreatedAtDesc();

    /**
     * Find accounts by type for a specific owner
     */
    List<Account> findByOwnerAndAccountTypeOrderByCreatedAtDesc(User owner, AccountType accountType);

    /**
     * Find accounts by name containing text (case-insensitive) for a specific owner
     */
    @Query("SELECT a FROM Account a WHERE a.owner.id = :ownerId AND LOWER(a.name) LIKE LOWER(CONCAT('%', :namePattern, '%')) ORDER BY a.createdAt DESC")
    List<Account> findByOwnerIdAndNameContainingIgnoreCase(@Param("ownerId") Long ownerId, @Param("namePattern") String namePattern);

    /**
     * Check if account name exists for a specific owner (case-insensitive)
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.owner.id = :ownerId AND LOWER(a.name) = LOWER(:name)")
    boolean existsByOwnerIdAndNameIgnoreCase(@Param("ownerId") Long ownerId, @Param("name") String name);

    /**
     * Check if account name exists for a specific owner excluding current account (for updates)
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a WHERE a.owner.id = :ownerId AND LOWER(a.name) = LOWER(:name) AND a.id != :accountId")
    boolean existsByOwnerIdAndNameIgnoreCaseAndIdNot(@Param("ownerId") Long ownerId, @Param("name") String name, @Param("accountId") Long accountId);

    /**
     * Update account balance
     */
    @Modifying
    @Query("UPDATE Account a SET a.currentBalance = :newBalance WHERE a.id = :accountId")
    void updateBalance(@Param("accountId") Long accountId, @Param("newBalance") BigDecimal newBalance);

    /**
     * Find account by ID and owner (for security)
     */
    Optional<Account> findByIdAndOwner(Long id, User owner);

    /**
     * Find account by ID and owner ID (for security)
     */
    @Query("SELECT a FROM Account a WHERE a.id = :accountId AND a.owner.id = :ownerId")
    Optional<Account> findByIdAndOwnerId(@Param("accountId") Long accountId, @Param("ownerId") Long ownerId);

    /**
     * Check if user has access to account (owner or has permission)
     */
    @Query("SELECT COUNT(a) > 0 FROM Account a LEFT JOIN AccountPermission ap ON a.id = ap.account.id " +
           "WHERE a.id = :accountId AND (a.owner.id = :userId OR (a.isShared = true AND ap.user.id = :userId))")
    boolean hasUserAccessToAccount(@Param("accountId") Long accountId, @Param("userId") Long userId);

    /**
     * Get total balance for all accounts owned by user
     */
    @Query("SELECT COALESCE(SUM(a.currentBalance), 0) FROM Account a WHERE a.owner.id = :ownerId")
    BigDecimal getTotalBalanceByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Get account count by owner
     */
    long countByOwner(User owner);

    /**
     * Get account count by owner ID
     */
    @Query("SELECT COUNT(a) FROM Account a WHERE a.owner.id = :ownerId")
    long countByOwnerId(@Param("ownerId") Long ownerId);

    /**
     * Find accounts with balance above threshold
     */
    @Query("SELECT a FROM Account a WHERE a.owner.id = :ownerId AND a.currentBalance >= :threshold ORDER BY a.currentBalance DESC")
    List<Account> findByOwnerIdAndBalanceGreaterThanEqual(@Param("ownerId") Long ownerId, @Param("threshold") BigDecimal threshold);

    /**
     * Find accounts with balance below threshold
     */
    @Query("SELECT a FROM Account a WHERE a.owner.id = :ownerId AND a.currentBalance <= :threshold ORDER BY a.currentBalance ASC")
    List<Account> findByOwnerIdAndBalanceLessThanEqual(@Param("ownerId") Long ownerId, @Param("threshold") BigDecimal threshold);
}