package com.checkbook.api.repository;

import com.checkbook.api.entity.PermissionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRequestRepository extends JpaRepository<PermissionRequest, Long> {

    /**
     * Find all permission requests for a specific account with pagination
     */
    Page<PermissionRequest> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    /**
     * Find all permission requests made by a specific user
     */
    Page<PermissionRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    /**
     * Find pending permission requests for an account
     */
    List<PermissionRequest> findByAccountIdAndStatusOrderByCreatedAtDesc(
        Long accountId,
        PermissionRequest.RequestStatus status
    );

    /**
     * Find pending permission requests for a specific user and account
     */
    Optional<PermissionRequest> findByAccountIdAndRequesterIdAndStatus(
        Long accountId,
        Long requesterId,
        PermissionRequest.RequestStatus status
    );

    /**
     * Check if there's already a pending request for the same user and account
     */
    boolean existsByAccountIdAndRequesterIdAndStatus(
        Long accountId,
        Long requesterId,
        PermissionRequest.RequestStatus status
    );

    /**
     * Find permission requests by status with pagination
     */
    Page<PermissionRequest> findByStatusOrderByCreatedAtDesc(
        PermissionRequest.RequestStatus status,
        Pageable pageable
    );

    /**
     * Find permission requests reviewed by a specific user
     */
    Page<PermissionRequest> findByReviewedByOrderByReviewedAtDesc(Long reviewerId, Pageable pageable);

    /**
     * Find permission requests for accounts owned by a specific user
     */
    @Query("SELECT pr FROM PermissionRequest pr " +
           "JOIN Account a ON pr.accountId = a.id " +
           "WHERE a.owner.id = :ownerId " +
           "ORDER BY pr.createdAt DESC")
    Page<PermissionRequest> findByAccountOwnerIdOrderByCreatedAtDesc(
        @Param("ownerId") Long ownerId,
        Pageable pageable
    );

    /**
     * Find pending permission requests for accounts owned by a specific user
     */
    @Query("SELECT pr FROM PermissionRequest pr " +
           "JOIN Account a ON pr.accountId = a.id " +
           "WHERE a.owner.id = :ownerId AND pr.status = :status " +
           "ORDER BY pr.createdAt DESC")
    List<PermissionRequest> findByAccountOwnerIdAndStatusOrderByCreatedAtDesc(
        @Param("ownerId") Long ownerId,
        @Param("status") PermissionRequest.RequestStatus status
    );

    /**
     * Count pending requests for an account
     */
    long countByAccountIdAndStatus(Long accountId, PermissionRequest.RequestStatus status);

    /**
     * Count pending requests for accounts owned by a user
     */
    @Query("SELECT COUNT(pr) FROM PermissionRequest pr " +
           "JOIN Account a ON pr.accountId = a.id " +
           "WHERE a.owner.id = :ownerId AND pr.status = :status")
    long countByAccountOwnerIdAndStatus(
        @Param("ownerId") Long ownerId,
        @Param("status") PermissionRequest.RequestStatus status
    );

    /**
     * Find recent permission requests (last N entries)
     */
    List<PermissionRequest> findTop10ByAccountIdOrderByCreatedAtDesc(Long accountId);

    /**
     * Find permission requests within a date range
     */
    @Query("SELECT pr FROM PermissionRequest pr " +
           "WHERE pr.accountId = :accountId " +
           "AND pr.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pr.createdAt DESC")
    Page<PermissionRequest> findByAccountIdAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Find permission requests with complex filtering
     */
    @Query("SELECT pr FROM PermissionRequest pr " +
           "WHERE pr.accountId = :accountId " +
           "AND (:status IS NULL OR pr.status = :status) " +
           "AND (:requesterId IS NULL OR pr.requesterId = :requesterId) " +
           "AND (:startDate IS NULL OR pr.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR pr.createdAt <= :endDate) " +
           "ORDER BY pr.createdAt DESC")
    Page<PermissionRequest> findWithFilters(
        @Param("accountId") Long accountId,
        @Param("status") PermissionRequest.RequestStatus status,
        @Param("requesterId") Long requesterId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    /**
     * Delete old processed requests (for maintenance/cleanup)
     */
    void deleteByStatusNotAndCreatedAtBefore(
        PermissionRequest.RequestStatus status,
        LocalDateTime cutoffDate
    );
}