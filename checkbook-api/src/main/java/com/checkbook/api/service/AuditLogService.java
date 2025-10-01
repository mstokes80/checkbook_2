package com.checkbook.api.service;

import com.checkbook.api.dto.ApiResponse;
import com.checkbook.api.entity.AuditLog;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionValidationService permissionValidationService;

    public void logPermissionGranted(Long accountId, User grantingUser, User targetUser, String permissionType, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("grantingUserId", grantingUser.getId());
        details.put("grantingUsername", grantingUser.getUsername());
        details.put("targetUserId", targetUser.getId());
        details.put("targetUsername", targetUser.getUsername());
        details.put("permissionType", permissionType);
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, grantingUser.getId(), AuditLog.ActionType.PERMISSION_GRANTED, details);
        logger.info("Audit: Permission '{}' granted to user '{}' for account {} by user '{}'",
                   permissionType, targetUser.getUsername(), accountId, grantingUser.getUsername());
    }

    public void logPermissionModified(Long accountId, User modifyingUser, User targetUser, String oldPermissionType, String newPermissionType, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("modifyingUserId", modifyingUser.getId());
        details.put("modifyingUsername", modifyingUser.getUsername());
        details.put("targetUserId", targetUser.getId());
        details.put("targetUsername", targetUser.getUsername());
        details.put("oldPermissionType", oldPermissionType);
        details.put("newPermissionType", newPermissionType);
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, modifyingUser.getId(), AuditLog.ActionType.PERMISSION_MODIFIED, details);
        logger.info("Audit: Permission modified from '{}' to '{}' for user '{}' on account {} by user '{}'",
                   oldPermissionType, newPermissionType, targetUser.getUsername(), accountId, modifyingUser.getUsername());
    }

    public void logPermissionRevoked(Long accountId, User revokingUser, User targetUser, String permissionType, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("revokingUserId", revokingUser.getId());
        details.put("revokingUsername", revokingUser.getUsername());
        details.put("targetUserId", targetUser.getId());
        details.put("targetUsername", targetUser.getUsername());
        details.put("permissionType", permissionType);
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, revokingUser.getId(), AuditLog.ActionType.PERMISSION_REVOKED, details);
        logger.info("Audit: Permission '{}' revoked from user '{}' for account {} by user '{}'",
                   permissionType, targetUser.getUsername(), accountId, revokingUser.getUsername());
    }

    public void logAccountViewed(Long accountId, User viewingUser, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("viewingUserId", viewingUser.getId());
        details.put("viewingUsername", viewingUser.getUsername());
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, viewingUser.getId(), AuditLog.ActionType.ACCOUNT_VIEWED, details);
        logger.debug("Audit: Account {} viewed by user '{}'", accountId, viewingUser.getUsername());
    }

    public void logTransactionAdded(Long accountId, User addingUser, Map<String, Object> transactionDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("addingUserId", addingUser.getId());
        details.put("addingUsername", addingUser.getUsername());
        if (transactionDetails != null) {
            details.putAll(transactionDetails);
        }

        createAuditLog(accountId, addingUser.getId(), AuditLog.ActionType.TRANSACTION_ADDED, details);
        logger.info("Audit: Transaction added to account {} by user '{}'", accountId, addingUser.getUsername());
    }

    public void logTransactionModified(Long accountId, User modifyingUser, Map<String, Object> transactionDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("modifyingUserId", modifyingUser.getId());
        details.put("modifyingUsername", modifyingUser.getUsername());
        if (transactionDetails != null) {
            details.putAll(transactionDetails);
        }

        createAuditLog(accountId, modifyingUser.getId(), AuditLog.ActionType.TRANSACTION_MODIFIED, details);
        logger.info("Audit: Transaction modified in account {} by user '{}'", accountId, modifyingUser.getUsername());
    }

    public void logTransactionDeleted(Long accountId, User deletingUser, Map<String, Object> transactionDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("deletingUserId", deletingUser.getId());
        details.put("deletingUsername", deletingUser.getUsername());
        if (transactionDetails != null) {
            details.putAll(transactionDetails);
        }

        createAuditLog(accountId, deletingUser.getId(), AuditLog.ActionType.TRANSACTION_DELETED, details);
        logger.info("Audit: Transaction deleted from account {} by user '{}'", accountId, deletingUser.getUsername());
    }

    public void logAccountModified(Long accountId, User modifyingUser, Map<String, Object> modificationDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("modifyingUserId", modifyingUser.getId());
        details.put("modifyingUsername", modifyingUser.getUsername());
        if (modificationDetails != null) {
            details.putAll(modificationDetails);
        }

        createAuditLog(accountId, modifyingUser.getId(), AuditLog.ActionType.ACCOUNT_MODIFIED, details);
        logger.info("Audit: Account {} modified by user '{}'", accountId, modifyingUser.getUsername());
    }

    public void logPermissionRequested(Long accountId, User requestingUser, String requestedPermission, String currentPermission, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("requestingUserId", requestingUser.getId());
        details.put("requestingUsername", requestingUser.getUsername());
        details.put("requestedPermission", requestedPermission);
        details.put("currentPermission", currentPermission);
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, requestingUser.getId(), AuditLog.ActionType.PERMISSION_REQUESTED, details);
        logger.info("Audit: Permission '{}' requested for account {} by user '{}'",
                   requestedPermission, accountId, requestingUser.getUsername());
    }

    public void logPermissionRequestApproved(Long accountId, User approvingUser, User requestingUser, String permissionType, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("approvingUserId", approvingUser.getId());
        details.put("approvingUsername", approvingUser.getUsername());
        details.put("requestingUserId", requestingUser.getId());
        details.put("requestingUsername", requestingUser.getUsername());
        details.put("permissionType", permissionType);
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, approvingUser.getId(), AuditLog.ActionType.PERMISSION_REQUEST_APPROVED, details);
        logger.info("Audit: Permission request for '{}' approved for user '{}' on account {} by user '{}'",
                   permissionType, requestingUser.getUsername(), accountId, approvingUser.getUsername());
    }

    public void logPermissionRequestDenied(Long accountId, User denyingUser, User requestingUser, String permissionType, String reason, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("denyingUserId", denyingUser.getId());
        details.put("denyingUsername", denyingUser.getUsername());
        details.put("requestingUserId", requestingUser.getId());
        details.put("requestingUsername", requestingUser.getUsername());
        details.put("permissionType", permissionType);
        details.put("reason", reason);
        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, denyingUser.getId(), AuditLog.ActionType.PERMISSION_REQUEST_DENIED, details);
        logger.info("Audit: Permission request for '{}' denied for user '{}' on account {} by user '{}'",
                   permissionType, requestingUser.getUsername(), accountId, denyingUser.getUsername());
    }

    public Page<AuditLog> getAuditLogsForAccount(Long accountId, Pageable pageable) {
        return auditLogRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
    }

    public Page<AuditLog> getAuditLogsForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<AuditLog> getAuditLogsWithFilters(Long accountId, AuditLog.ActionType actionType, Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        // Choose the appropriate method based on which filters are provided
        boolean hasActionType = actionType != null;
        boolean hasUserId = userId != null;
        boolean hasDateRange = startDate != null && endDate != null;

        if (hasActionType && hasUserId && hasDateRange) {
            return auditLogRepository.findByAccountIdAndActionTypeAndUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                accountId, actionType, userId, startDate, endDate, pageable);
        } else if (hasActionType && hasDateRange) {
            return auditLogRepository.findByAccountIdAndActionTypeAndCreatedAtBetweenOrderByCreatedAtDesc(
                accountId, actionType, startDate, endDate, pageable);
        } else if (hasUserId && hasDateRange) {
            return auditLogRepository.findByAccountIdAndUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                accountId, userId, startDate, endDate, pageable);
        } else if (hasActionType && hasUserId) {
            return auditLogRepository.findByAccountIdAndActionTypeAndUserIdOrderByCreatedAtDesc(
                accountId, actionType, userId, pageable);
        } else if (hasActionType) {
            return auditLogRepository.findByAccountIdAndActionTypeOrderByCreatedAtDesc(
                accountId, actionType, pageable);
        } else if (hasDateRange) {
            return auditLogRepository.findByAccountIdAndDateRange(accountId, startDate, endDate, pageable);
        } else if (hasUserId) {
            return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else {
            // No filters, just return all for the account
            return auditLogRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        }
    }

    public List<AuditLog> getRecentAuditLogs(Long accountId) {
        return auditLogRepository.findTop10ByAccountIdOrderByCreatedAtDesc(accountId);
    }

    public Long countAuditLogsInDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.countByAccountIdAndDateRange(accountId, startDate, endDate);
    }

    public ApiResponse getAccountAuditLogs(Long accountId, String actionType, Long userId,
                                           LocalDateTime startDate, LocalDateTime endDate,
                                           User currentUser, Pageable pageable) {
        try {
            // Check if user has access to this account
            if (!permissionValidationService.hasAccountAccess(currentUser, accountId)) {
                return ApiResponse.error("Access denied - you don't have permission to view audit logs for this account");
            }

            // Parse action type if provided
            AuditLog.ActionType parsedActionType = null;
            if (actionType != null && !actionType.isEmpty()) {
                try {
                    parsedActionType = AuditLog.ActionType.valueOf(actionType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ApiResponse.error("Invalid action type. Valid values are: PERMISSION_GRANTED, PERMISSION_MODIFIED, PERMISSION_REVOKED, ACCOUNT_VIEWED, TRANSACTION_ADDED, TRANSACTION_MODIFIED, TRANSACTION_DELETED, ACCOUNT_MODIFIED, PERMISSION_REQUESTED, PERMISSION_REQUEST_APPROVED, PERMISSION_REQUEST_DENIED");
                }
            }

            // Get audit logs with filters
            Page<AuditLog> auditLogsPage = getAuditLogsWithFilters(accountId, parsedActionType, userId,
                                                                  startDate, endDate, pageable);

            logger.debug("Retrieved {} audit logs for account {} (page {}/{})",
                        auditLogsPage.getNumberOfElements(), accountId,
                        auditLogsPage.getNumber() + 1, auditLogsPage.getTotalPages());

            return ApiResponse.success("Audit logs retrieved successfully", auditLogsPage);

        } catch (Exception e) {
            logger.error("Error retrieving audit logs for account {}: {}", accountId, e.getMessage());
            return ApiResponse.error("Failed to retrieve audit logs: " + e.getMessage());
        }
    }

    public void cleanupOldAuditLogs(LocalDateTime cutoffDate) {
        logger.info("Cleaning up audit logs older than {}", cutoffDate);
        auditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        logger.info("Completed cleanup of old audit logs");
    }

    private void createAuditLog(Long accountId, Long userId, AuditLog.ActionType actionType, Map<String, Object> details) {
        try {
            String detailsJson = null;
            if (details != null && !details.isEmpty()) {
                detailsJson = objectMapper.writeValueAsString(details);
            }

            String ipAddress = null;
            String userAgent = null;

            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIpAddress(request);
                userAgent = request.getHeader("User-Agent");
            } catch (IllegalStateException e) {
                logger.debug("No HTTP request context available for audit log");
            }

            AuditLog auditLog = new AuditLog(accountId, userId, actionType, detailsJson, ipAddress, userAgent);
            auditLogRepository.save(auditLog);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize audit log details to JSON: {}", e.getMessage());
            AuditLog auditLog = new AuditLog(accountId, userId, actionType, null, null, null);
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }
}