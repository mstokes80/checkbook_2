package com.checkbook.api.repository;

import com.checkbook.api.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find valid (unused and not expired) token
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token AND t.isUsed = false AND t.expiresAt > :currentTime")
    Optional<PasswordResetToken> findValidToken(@Param("token") String token, @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find token by token string
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find all tokens for a user
     */
    List<PasswordResetToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find valid tokens for a user (unused and not expired)
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.userId = :userId AND t.isUsed = false AND t.expiresAt > :currentTime ORDER BY t.createdAt DESC")
    List<PasswordResetToken> findValidTokensByUserId(@Param("userId") Long userId, @Param("currentTime") LocalDateTime currentTime);

    /**
     * Mark all existing tokens for a user as used (when generating new token)
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.isUsed = true, t.usedAt = :usedAt WHERE t.userId = :userId AND t.isUsed = false")
    void markAllUserTokensAsUsed(@Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoffTime")
    void deleteExpiredTokens(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count valid tokens for user in last hour (rate limiting)
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.userId = :userId AND t.createdAt > :cutoffTime")
    Long countRecentTokensForUser(@Param("userId") Long userId, @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count valid tokens from IP in last hour (rate limiting)
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.requestIp = :ip AND t.createdAt > :cutoffTime")
    Long countRecentTokensFromIp(@Param("ip") String ip, @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find tokens that need cleanup (expired and older than X days)
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.expiresAt < :cutoffTime")
    List<PasswordResetToken> findTokensForCleanup(@Param("cutoffTime") LocalDateTime cutoffTime);
}