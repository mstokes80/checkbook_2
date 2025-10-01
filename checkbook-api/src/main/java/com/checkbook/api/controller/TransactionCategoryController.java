package com.checkbook.api.controller;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.User;
import com.checkbook.api.service.TransactionCategoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Transaction Category Management
 * Handles category CRUD operations, search, and validation
 */
@RestController
@RequestMapping("/transaction-categories")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TransactionCategoryController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCategoryController.class);

    @Autowired
    private TransactionCategoryService transactionCategoryService;

    /**
     * Create a new transaction category
     * POST /api/transaction-categories
     */
    @PostMapping
    public ResponseEntity<ApiResponse> createCategory(
            @Valid @RequestBody CreateTransactionCategoryRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Creating transaction category '{}' by user '{}'", request.getName(), currentUser.getUsername());

        ApiResponse response = transactionCategoryService.createCategory(request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Update an existing transaction category
     * PUT /api/transaction-categories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionCategoryRequest request,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Updating transaction category {} by user '{}'", id, currentUser.getUsername());

        ApiResponse response = transactionCategoryService.updateCategory(id, request, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete a transaction category
     * DELETE /api/transaction-categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.info("Deleting transaction category {} by user '{}'", id, currentUser.getUsername());

        ApiResponse response = transactionCategoryService.deleteCategory(id, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get transaction category by ID
     * GET /api/transaction-categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getCategory(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving transaction category {} for user '{}'", id, currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getCategory(id, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get all transaction categories
     * GET /api/transaction-categories
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getAllCategories(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving all transaction categories for user '{}'", currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getAllCategories(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get system default categories
     * GET /api/transaction-categories/system
     */
    @GetMapping("/system")
    public ResponseEntity<ApiResponse> getSystemCategories(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving system transaction categories for user '{}'", currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getSystemCategories(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get user-created categories
     * GET /api/transaction-categories/user
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse> getUserCategories(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving user-created transaction categories for user '{}'", currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getUserCategories(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Search categories by name
     * GET /api/transaction-categories/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchCategories(
            @RequestParam(name = "q", required = false) String searchTerm,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Searching transaction categories with term '{}' for user '{}'", searchTerm, currentUser.getUsername());

        ApiResponse response = transactionCategoryService.searchCategories(searchTerm, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get categories with transaction counts
     * GET /api/transaction-categories/with-counts
     */
    @GetMapping("/with-counts")
    public ResponseEntity<ApiResponse> getCategoriesWithCounts(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving transaction categories with counts for user '{}'", currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getCategoriesWithCounts(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get categories that can be deleted
     * GET /api/transaction-categories/deletable
     */
    @GetMapping("/deletable")
    public ResponseEntity<ApiResponse> getDeletableCategories(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving deletable transaction categories for user '{}'", currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getDeletableCategories(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get most used categories
     * GET /api/transaction-categories/most-used?limit={limit}
     */
    @GetMapping("/most-used")
    public ResponseEntity<ApiResponse> getMostUsedCategories(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving top {} most used transaction categories for user '{}'", limit, currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getMostUsedCategories(currentUser, limit);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get category summary statistics
     * GET /api/transaction-categories/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse> getCategorySummary(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving category summary statistics for user '{}'", currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getCategorySummary(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get default uncategorized category
     * GET /api/transaction-categories/uncategorized
     */
    @GetMapping("/uncategorized")
    public ResponseEntity<ApiResponse> getUncategorizedCategory(Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Retrieving uncategorized category for user '{}'", currentUser.getUsername());

        ApiResponse response = transactionCategoryService.getUncategorizedCategory(currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Validate if a category can be deleted
     * GET /api/transaction-categories/{id}/validate-deletion
     */
    @GetMapping("/{id}/validate-deletion")
    public ResponseEntity<ApiResponse> validateCategoryDeletion(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getCurrentUser(authentication);
        logger.debug("Validating deletion of category {} for user '{}'", id, currentUser.getUsername());

        ApiResponse response = transactionCategoryService.validateCategoryDeletion(id, currentUser);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else if (response.getMessage().contains("not found")) {
            return ResponseEntity.notFound().build();
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
}