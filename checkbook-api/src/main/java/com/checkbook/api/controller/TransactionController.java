package com.checkbook.api.controller;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.User;
import com.checkbook.api.service.TransactionService;
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

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for Transaction Management
 * Handles transaction CRUD operations, filtering, and statistics
 */
@RestController
@RequestMapping("/transactions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    /**
     * Create a new transaction
     * POST /api/transactions
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Creating transaction for account {} by user '{}'", request.getAccountId(), currentUser.getUsername());

        ApiResponse response = transactionService.createTransaction(request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update an existing transaction
     * PUT /api/transactions/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Updating transaction {} by user '{}'", id, currentUser.getUsername());

        ApiResponse response = transactionService.updateTransaction(id, request, currentUser);

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
     * Delete a transaction
     * DELETE /api/transactions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Deleting transaction {} by user '{}'", id, currentUser.getUsername());

        ApiResponse response = transactionService.deleteTransaction(id, currentUser);

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
     * Get transaction by ID
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving transaction {} for user '{}'", id, currentUser.getUsername());

        ApiResponse response = transactionService.getTransaction(id, currentUser);

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
     * Get transactions for an account with filtering and pagination
     * GET /api/transactions?accountId={accountId}&categoryId={categoryId}&startDate={startDate}&endDate={endDate}&search={search}&page={page}&size={size}&sort={sort}
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAccountTransactions(
            @RequestParam Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving transactions for account {} by user '{}'", accountId, currentUser.getUsername());

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        ApiResponse response = transactionService.getAccountTransactions(
                accountId, categoryId, startDate, endDate, search, currentUser, pageable);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get recent transactions for an account
     * GET /api/transactions/recent?accountId={accountId}&limit={limit}
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse> getRecentTransactions(
            @RequestParam Long accountId,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving {} recent transactions for account {} by user '{}'",
                    limit, accountId, currentUser.getUsername());

        ApiResponse response = transactionService.getRecentTransactions(accountId, limit, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get transaction statistics for an account
     * GET /api/transactions/statistics?accountId={accountId}&startDate={startDate}&endDate={endDate}
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse> getTransactionStatistics(
            @RequestParam Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving transaction statistics for account {} by user '{}'",
                    accountId, currentUser.getUsername());

        ApiResponse response = transactionService.getTransactionStatistics(accountId, startDate, endDate, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get uncategorized transactions for an account
     * GET /api/transactions/uncategorized?accountId={accountId}&page={page}&size={size}
     */
    @GetMapping("/uncategorized")
    public ResponseEntity<ApiResponse> getUncategorizedTransactions(
            @RequestParam Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving uncategorized transactions for account {} by user '{}'",
                    accountId, currentUser.getUsername());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        ApiResponse response = transactionService.getUncategorizedTransactions(accountId, currentUser, pageable);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Bulk categorize transactions
     * POST /api/transactions/bulk-categorize
     */
    @PostMapping("/bulk-categorize")
    public ResponseEntity<ApiResponse> bulkCategorizeTransactions(
            @RequestBody BulkCategorizeRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Bulk categorizing {} transactions to category {} by user '{}'",
                   request.getTransactionIds().size(), request.getCategoryId(), currentUser.getUsername());

        ApiResponse response = transactionService.bulkCategorizeTransactions(
                request.getTransactionIds(), request.getCategoryId(), currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("Access denied")) {
            return ResponseEntity.status(403).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }

    /**
     * Request DTO for bulk categorization
     */
    public static class BulkCategorizeRequest {
        private List<Long> transactionIds;
        private Long categoryId;

        public BulkCategorizeRequest() {}

        public BulkCategorizeRequest(List<Long> transactionIds, Long categoryId) {
            this.transactionIds = transactionIds;
            this.categoryId = categoryId;
        }

        public List<Long> getTransactionIds() {
            return transactionIds;
        }

        public void setTransactionIds(List<Long> transactionIds) {
            this.transactionIds = transactionIds;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }
    }
}