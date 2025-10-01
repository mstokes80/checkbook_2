package com.checkbook.api.repository;

import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.AccountPermission;
import com.checkbook.api.entity.PermissionType;
import com.checkbook.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountPermissionRepository extends JpaRepository<AccountPermission, Long> {

    /**
     * Find all permissions for a specific account
     */
    List<AccountPermission> findByAccountOrderByCreatedAtDesc(Account account);

    /**
     * Find all permissions for a specific account ID
     */
    @Query("SELECT ap FROM AccountPermission ap WHERE ap.account.id = :accountId ORDER BY ap.createdAt DESC")
    List<AccountPermission> findByAccountIdOrderByCreatedAtDesc(@Param("accountId") Long accountId);

    /**
     * Find all permissions for a specific user
     */
    List<AccountPermission> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all permissions for a specific user ID
     */
    @Query("SELECT ap FROM AccountPermission ap WHERE ap.user.id = :userId ORDER BY ap.createdAt DESC")
    List<AccountPermission> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Find permission for specific account and user
     */
    Optional<AccountPermission> findByAccountAndUser(Account account, User user);

    /**
     * Find permission for specific account ID and user ID
     */
    @Query("SELECT ap FROM AccountPermission ap WHERE ap.account.id = :accountId AND ap.user.id = :userId")
    Optional<AccountPermission> findByAccountIdAndUserId(@Param("accountId") Long accountId, @Param("userId") Long userId);

    /**
     * Check if user has permission for account
     */
    @Query("SELECT COUNT(ap) > 0 FROM AccountPermission ap WHERE ap.account.id = :accountId AND ap.user.id = :userId")
    boolean existsByAccountIdAndUserId(@Param("accountId") Long accountId, @Param("userId") Long userId);

    /**
     * Check if user has specific permission type for account
     */
    @Query("SELECT COUNT(ap) > 0 FROM AccountPermission ap WHERE ap.account.id = :accountId AND ap.user.id = :userId AND ap.permissionType = :permissionType")
    boolean existsByAccountIdAndUserIdAndPermissionType(@Param("accountId") Long accountId, @Param("userId") Long userId, @Param("permissionType") PermissionType permissionType);

    /**
     * Find accounts with FULL_ACCESS permission for a user
     */
    @Query("SELECT ap FROM AccountPermission ap WHERE ap.user.id = :userId AND ap.permissionType = 'FULL_ACCESS' ORDER BY ap.createdAt DESC")
    List<AccountPermission> findFullAccessPermissionsByUserId(@Param("userId") Long userId);

    /**
     * Find accounts with VIEW_ONLY permission for a user
     */
    @Query("SELECT ap FROM AccountPermission ap WHERE ap.user.id = :userId AND ap.permissionType = 'VIEW_ONLY' ORDER BY ap.createdAt DESC")
    List<AccountPermission> findViewOnlyPermissionsByUserId(@Param("userId") Long userId);

    /**
     * Find all permissions by permission type
     */
    List<AccountPermission> findByPermissionTypeOrderByCreatedAtDesc(PermissionType permissionType);

    /**
     * Update permission type for specific account and user
     */
    @Modifying
    @Query("UPDATE AccountPermission ap SET ap.permissionType = :permissionType WHERE ap.account.id = :accountId AND ap.user.id = :userId")
    void updatePermissionType(@Param("accountId") Long accountId, @Param("userId") Long userId, @Param("permissionType") PermissionType permissionType);

    /**
     * Delete permission for specific account and user
     */
    @Modifying
    @Query("DELETE FROM AccountPermission ap WHERE ap.account.id = :accountId AND ap.user.id = :userId")
    void deleteByAccountIdAndUserId(@Param("accountId") Long accountId, @Param("userId") Long userId);

    /**
     * Delete all permissions for an account
     */
    @Modifying
    @Query("DELETE FROM AccountPermission ap WHERE ap.account.id = :accountId")
    void deleteByAccountId(@Param("accountId") Long accountId);

    /**
     * Delete all permissions for a user
     */
    @Modifying
    @Query("DELETE FROM AccountPermission ap WHERE ap.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Count permissions for an account
     */
    @Query("SELECT COUNT(ap) FROM AccountPermission ap WHERE ap.account.id = :accountId")
    long countByAccountId(@Param("accountId") Long accountId);

    /**
     * Count permissions for a user
     */
    @Query("SELECT COUNT(ap) FROM AccountPermission ap WHERE ap.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Find users with permission to a specific account
     */
    @Query("SELECT ap.user FROM AccountPermission ap WHERE ap.account.id = :accountId ORDER BY ap.createdAt DESC")
    List<User> findUsersByAccountId(@Param("accountId") Long accountId);

    /**
     * Find accounts accessible by a user (through permissions)
     */
    @Query("SELECT ap.account FROM AccountPermission ap WHERE ap.user.id = :userId ORDER BY ap.createdAt DESC")
    List<Account> findAccountsByUserId(@Param("userId") Long userId);

    /**
     * Check if user has any access (VIEW_ONLY or FULL_ACCESS) to account
     */
    @Query("SELECT ap.permissionType FROM AccountPermission ap WHERE ap.account.id = :accountId AND ap.user.id = :userId")
    Optional<PermissionType> findPermissionTypeByAccountIdAndUserId(@Param("accountId") Long accountId, @Param("userId") Long userId);

    /**
     * Find all permissions for accounts owned by a specific user
     */
    @Query("SELECT ap FROM AccountPermission ap WHERE ap.account.owner.id = :ownerId ORDER BY ap.createdAt DESC")
    List<AccountPermission> findPermissionsForAccountsOwnedBy(@Param("ownerId") Long ownerId);
}