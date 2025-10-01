package com.checkbook.api.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "permission_requests")
@EntityListeners(AuditingEntityListener.class)
public class PermissionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_permission", nullable = false, length = 20)
    private PermissionType requestedPermission;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_permission", length = 20)
    private PermissionType currentPermission;

    @Column(name = "request_message", columnDefinition = "TEXT")
    private String requestMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "review_message", columnDefinition = "TEXT")
    private String reviewMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", insertable = false, updatable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", insertable = false, updatable = false)
    private User reviewer;

    // Constructors
    public PermissionRequest() {}

    public PermissionRequest(Long accountId, Long requesterId, PermissionType requestedPermission, PermissionType currentPermission, String requestMessage) {
        this.accountId = accountId;
        this.requesterId = requesterId;
        this.requestedPermission = requestedPermission;
        this.currentPermission = currentPermission;
        this.requestMessage = requestMessage;
        this.status = RequestStatus.PENDING;
    }

    // Business methods
    public void approve(Long reviewerId, String reviewMessage) {
        this.status = RequestStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewMessage = reviewMessage;
        this.reviewedAt = LocalDateTime.now();
    }

    public void deny(Long reviewerId, String reviewMessage) {
        this.status = RequestStatus.DENIED;
        this.reviewedBy = reviewerId;
        this.reviewMessage = reviewMessage;
        this.reviewedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = RequestStatus.CANCELLED;
        this.reviewedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == RequestStatus.PENDING;
    }

    public boolean isApproved() {
        return this.status == RequestStatus.APPROVED;
    }

    public boolean isDenied() {
        return this.status == RequestStatus.DENIED;
    }

    public boolean isCancelled() {
        return this.status == RequestStatus.CANCELLED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public PermissionType getRequestedPermission() {
        return requestedPermission;
    }

    public void setRequestedPermission(PermissionType requestedPermission) {
        this.requestedPermission = requestedPermission;
    }

    public PermissionType getCurrentPermission() {
        return currentPermission;
    }

    public void setCurrentPermission(PermissionType currentPermission) {
        this.currentPermission = currentPermission;
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewMessage() {
        return reviewMessage;
    }

    public void setReviewMessage(String reviewMessage) {
        this.reviewMessage = reviewMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    // Enum for request status
    public enum RequestStatus {
        PENDING,
        APPROVED,
        DENIED,
        CANCELLED
    }
}