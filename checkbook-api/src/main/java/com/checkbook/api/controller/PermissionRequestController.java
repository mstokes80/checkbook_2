package com.checkbook.api.controller;

import com.checkbook.api.dto.ApiResponse;
import com.checkbook.api.dto.PermissionRequestDto;
import com.checkbook.api.dto.ReviewMessage;
import com.checkbook.api.entity.User;
import com.checkbook.api.service.PermissionRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for Permission Request Management
 * Handles permission request CRUD operations, workflow management, and approval processes
 */
@RestController
@RequestMapping("/permission-requests")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PermissionRequestController {

    private static final Logger logger = LoggerFactory.getLogger(PermissionRequestController.class);

    @Autowired
    private PermissionRequestService permissionRequestService;

    /**
     * Create a new permission request
     * POST /api/permission-requests
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createPermissionRequest(
            @Valid @RequestBody PermissionRequestDto request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Creating permission request for account {} by user '{}'",
                   request.getAccountId(), currentUser.getUsername());

        ApiResponse response = permissionRequestService.createPermissionRequest(
                request.getAccountId(), request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get permission requests for current user
     * GET /api/permission-requests/my-requests
     */
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse> getMyPermissionRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving permission requests for user '{}'", currentUser.getUsername());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        ApiResponse response = permissionRequestService.getUserPermissionRequests(currentUser, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Get pending permission requests for accounts owned by current user
     * GET /api/permission-requests/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse> getPendingRequests(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving pending permission requests for account owner '{}'", currentUser.getUsername());

        ApiResponse response = permissionRequestService.getPendingPermissionRequests(currentUser);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all permission requests for accounts owned by current user
     * GET /api/permission-requests/for-my-accounts
     */
    @GetMapping("/for-my-accounts")
    public ResponseEntity<ApiResponse> getRequestsForMyAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving permission requests for accounts owned by '{}'", currentUser.getUsername());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        ApiResponse response = permissionRequestService.getAccountOwnerPermissionRequests(currentUser, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Approve a permission request for a specific account
     * PUT /api/permission-requests/account/{accountId}/{requestId}/approve
     */
    @PutMapping("/account/{accountId}/{requestId}/approve")
    public ResponseEntity<ApiResponse> approvePermissionRequest(
            @PathVariable Long accountId,
            @PathVariable Long requestId,
            @RequestBody(required = false) ReviewMessage reviewMessage,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Approving permission request {} for account {} by user '{}'",
                   requestId, accountId, currentUser.getUsername());

        String message = reviewMessage != null ? reviewMessage.getMessage() : "";
        ApiResponse response = permissionRequestService.approvePermissionRequest(
                accountId, requestId, message, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Deny a permission request for a specific account
     * PUT /api/permission-requests/account/{accountId}/{requestId}/deny
     */
    @PutMapping("/account/{accountId}/{requestId}/deny")
    public ResponseEntity<ApiResponse> denyPermissionRequest(
            @PathVariable Long accountId,
            @PathVariable Long requestId,
            @RequestBody(required = false) ReviewMessage reviewMessage,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Denying permission request {} for account {} by user '{}'",
                   requestId, accountId, currentUser.getUsername());

        String message = reviewMessage != null ? reviewMessage.getMessage() : "";
        ApiResponse response = permissionRequestService.denyPermissionRequest(
                accountId, requestId, message, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Cancel a permission request (by the requester)
     * PUT /api/permission-requests/{requestId}/cancel
     */
    @PutMapping("/{requestId}/cancel")
    public ResponseEntity<ApiResponse> cancelPermissionRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Cancelling permission request {} by user '{}'", requestId, currentUser.getUsername());

        ApiResponse response = permissionRequestService.cancelPermissionRequest(requestId, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get permission requests for a specific account with filtering
     * GET /api/permission-requests/account/{accountId}
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse> getAccountPermissionRequests(
            @PathVariable Long accountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving permission requests for account {} with filters", accountId);

        // Parse status enum if provided
        com.checkbook.api.entity.PermissionRequest.RequestStatus requestStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                requestStatus = com.checkbook.api.entity.PermissionRequest.RequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Invalid status value. Valid values are: PENDING, APPROVED, DENIED, CANCELLED"));
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        ApiResponse response = permissionRequestService.getPermissionRequestWithFilters(
                accountId, requestStatus, requesterId, startDate, endDate, currentUser, pageable);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        throw new RuntimeException("Invalid authentication principal");
    }
}