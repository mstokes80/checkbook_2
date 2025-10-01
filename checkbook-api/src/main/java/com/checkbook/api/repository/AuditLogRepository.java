package com.checkbook.api.repository;

import com.checkbook.api.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific account with pagination
     */
    Page<AuditLog> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    /**
     * Find all audit logs for a specific user with pagination
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs for an account within a date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.accountId = :accountId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find audit logs for an account by action type
     */
    Page<AuditLog> findByAccountIdAndActionTypeOrderByCreatedAtDesc(
        Long accountId,
        AuditLog.ActionType actionType,
        Pageable pageable
    );

    /**
     * Find audit logs for an account by user and date range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.accountId = :accountId " +
           "AND a.userId = :userId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByAccountIdAndUserIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find recent audit logs for an account (last N entries)
     */
    List<AuditLog> findTop10ByAccountIdOrderByCreatedAtDesc(Long accountId);

    /**
     * Count audit logs for an account within a date range
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.accountId = :accountId " +
           "AND a.createdAt BETWEEN :startDate AND :endDate")
    Long countByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find audit logs for an account by action type, user, and date range (with nulls allowed)
     */
    Page<AuditLog> findByAccountIdAndActionTypeAndUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long accountId,
        AuditLog.ActionType actionType,
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find audit logs for an account by action type and date range (with nulls allowed)
     */
    Page<AuditLog> findByAccountIdAndActionTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long accountId,
        AuditLog.ActionType actionType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find audit logs for an account by user and date range (with nulls allowed)
     */
    Page<AuditLog> findByAccountIdAndUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long accountId,
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find audit logs for an account by action type and user (with nulls allowed)
     */
    Page<AuditLog> findByAccountIdAndActionTypeAndUserIdOrderByCreatedAtDesc(
        Long accountId,
        AuditLog.ActionType actionType,
        Long userId,
        Pageable pageable
    );

    /**
     * Delete old audit logs (for maintenance/cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}