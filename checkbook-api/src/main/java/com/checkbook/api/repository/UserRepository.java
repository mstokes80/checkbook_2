package com.checkbook.api.repository;

import com.checkbook.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username (case-insensitive)
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Find user by email (case-insensitive)
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Find user by username or email (for login)
     */
    @Query("SELECT u FROM User u WHERE (LOWER(u.username) = LOWER(:usernameOrEmail) OR LOWER(u.email) = LOWER(:usernameOrEmail))")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    /**
     * Check if username exists (case-insensitive)
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Check if email exists (case-insensitive)
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Find enabled users only
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.accountLocked = false")
    Optional<User> findByUsernameIgnoreCaseAndEnabledTrueAndAccountLockedFalse(String username);

    /**
     * Update failed login attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1, u.lastLoginAttemptAt = :attemptTime WHERE u.id = :userId")
    void incrementFailedLoginAttempts(@Param("userId") Long userId, @Param("attemptTime") LocalDateTime attemptTime);

    /**
     * Reset failed login attempts after successful login
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lastSuccessfulLoginAt = :loginTime, u.lastLoginAttemptAt = :loginTime WHERE u.id = :userId")
    void resetFailedLoginAttempts(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Lock user account
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = true WHERE u.id = :userId")
    void lockAccount(@Param("userId") Long userId);

    /**
     * Unlock user account and reset failed attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.accountLocked = false, u.failedLoginAttempts = 0 WHERE u.id = :userId")
    void unlockAccount(@Param("userId") Long userId);

    /**
     * Verify user email
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    void verifyEmail(@Param("userId") Long userId);

    /**
     * Update user password
     */
    @Modifying
    @Query("UPDATE User u SET u.passwordHash = :passwordHash WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    /**
     * Find users with failed login attempts above threshold
     */
    @Query("SELECT u FROM User u WHERE u.failedLoginAttempts >= :threshold AND u.accountLocked = false")
    java.util.List<User> findUsersWithHighFailedAttempts(@Param("threshold") Integer threshold);

    /**
     * Find users that haven't verified their email within timeframe
     */
    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.createdAt < :cutoffTime")
    java.util.List<User> findUnverifiedUsersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
}