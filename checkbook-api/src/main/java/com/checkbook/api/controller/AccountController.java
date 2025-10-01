package com.checkbook.api.controller;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.User;
import com.checkbook.api.service.AccountService;
import com.checkbook.api.service.AuditLogService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST Controller for Account Management
 * Handles account CRUD operations, permission management, and dashboard functionality
 */
@RestController
@RequestMapping("/accounts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Create a new account
     * POST /api/accounts
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Creating account '{}' for user '{}'", request.getName(), currentUser.getUsername());

        ApiResponse response = accountService.createAccount(request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all accounts accessible by current user
     * GET /api/accounts
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAccessibleAccounts(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving accounts for user '{}'", currentUser.getUsername());

        ApiResponse response = accountService.getAccessibleAccounts(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get account by ID
     * GET /api/accounts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getAccountById(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving account {} for user '{}'", id, currentUser.getUsername());

        ApiResponse response = accountService.getAccountById(id, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update an existing account
     * PUT /api/accounts/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Updating account {} for user '{}'", id, currentUser.getUsername());

        ApiResponse response = accountService.updateAccount(id, request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete an account
     * DELETE /api/accounts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteAccount(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Deleting account {} for user '{}'", id, currentUser.getUsername());

        ApiResponse response = accountService.deleteAccount(id, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update account balance
     * PATCH /api/accounts/{id}/balance
     */
    @PatchMapping("/{id}/balance")
    public ResponseEntity<ApiResponse> updateAccountBalance(
            @PathVariable Long id,
            @RequestBody Map<String, BigDecimal> balanceRequest,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        BigDecimal newBalance = balanceRequest.get("balance");

        if (newBalance == null) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Balance is required"));
        }

        logger.info("Updating balance for account {} to {} by user '{}'",
                   id, newBalance, currentUser.getUsername());

        ApiResponse response = accountService.updateAccountBalance(id, newBalance, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Grant permission to user for shared account
     * POST /api/accounts/{id}/permissions
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse> grantPermission(
            @PathVariable Long id,
            @Valid @RequestBody AccountPermissionRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Granting {} permission to user '{}' for account {} by owner '{}'",
                   request.getPermissionType(), request.getUsernameOrEmail(),
                   id, currentUser.getUsername());

        ApiResponse response = accountService.grantPermission(id, request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("don't own")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all permissions for an account
     * GET /api/accounts/{id}/permissions
     */
    @GetMapping("/{id}/permissions")
    public ResponseEntity<ApiResponse> getAccountPermissions(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving permissions for account {} by user '{}'", id, currentUser.getUsername());

        ApiResponse response = accountService.getAccountPermissions(id, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("don't own")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Revoke permission from user for shared account
     * DELETE /api/accounts/{accountId}/permissions/{userId}
     */
    @DeleteMapping("/{accountId}/permissions/{userId}")
    public ResponseEntity<ApiResponse> revokePermission(
            @PathVariable Long accountId,
            @PathVariable Long userId,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Revoking permission for user {} from account {} by owner '{}'",
                   userId, accountId, currentUser.getUsername());

        ApiResponse response = accountService.revokePermission(accountId, userId, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("don't own")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update permission for a user on a shared account
     * PUT /api/accounts/{accountId}/permissions/{userId}
     */
    @PutMapping("/{accountId}/permissions/{userId}")
    public ResponseEntity<ApiResponse> updatePermission(
            @PathVariable Long accountId,
            @PathVariable Long userId,
            @Valid @RequestBody AccountPermissionRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Updating permission for user {} on account {} to {} by owner '{}'",
                   userId, accountId, request.getPermissionType(), currentUser.getUsername());

        // For updating permissions, we'll use the same grant permission logic
        // which handles both creating new and updating existing permissions
        ApiResponse response = accountService.grantPermission(accountId, request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("don't own")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get audit logs for an account with filtering and pagination
     * GET /api/accounts/{id}/audit-logs
     */
    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<ApiResponse> getAccountAuditLogs(
            @PathVariable Long id,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving audit logs for account {} with filters by user '{}'", id, currentUser.getUsername());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        ApiResponse response = auditLogService.getAccountAuditLogs(id, actionType, userId, startDate, endDate, currentUser, pageable);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get account dashboard summary
     * GET /api/accounts/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> getAccountDashboard(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving dashboard for user '{}'", currentUser.getUsername());

        // This will be implemented as part of dashboard functionality
        ApiResponse accountsResponse = accountService.getAccessibleAccounts(currentUser);

        if (accountsResponse.isSuccess()) {
            // For now, return the accounts list as dashboard data
            // This can be enhanced later with additional dashboard metrics
            return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully",
                                                         accountsResponse.getData()));
        } else {
            return ResponseEntity.badRequest().body(accountsResponse);
        }
    }

    /**
     * Get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}