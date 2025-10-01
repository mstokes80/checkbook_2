package com.checkbook.api.dto;

import com.checkbook.api.entity.PermissionRequest;
import com.checkbook.api.entity.PermissionType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class PermissionRequestDto {

    private Long id;

    @NotNull(message = "Account ID is required")
    private Long accountId;

    private Long requesterId;

    @NotNull(message = "Requested permission is required")
    private PermissionType requestedPermission;

    private PermissionType currentPermission;

    private String requestMessage;

    private PermissionRequest.RequestStatus status;

    private Long reviewedBy;

    private String reviewMessage;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    private String accountName;

    private String requesterUsername;

    private String requesterEmail;

    private String requesterFullName;

    private String reviewerUsername;

    private String reviewerFullName;

    public PermissionRequestDto() {}

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

    public PermissionRequest.RequestStatus getStatus() {
        return status;
    }

    public void setStatus(PermissionRequest.RequestStatus status) {
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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getRequesterUsername() {
        return requesterUsername;
    }

    public void setRequesterUsername(String requesterUsername) {
        this.requesterUsername = requesterUsername;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getRequesterFullName() {
        return requesterFullName;
    }

    public void setRequesterFullName(String requesterFullName) {
        this.requesterFullName = requesterFullName;
    }

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public String getReviewerFullName() {
        return reviewerFullName;
    }

    public void setReviewerFullName(String reviewerFullName) {
        this.reviewerFullName = reviewerFullName;
    }
}