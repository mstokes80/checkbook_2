package com.checkbook.api.service;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.TransactionCategory;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.TransactionCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCategoryService.class);

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    /**
     * Create a new transaction category
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse createCategory(CreateTransactionCategoryRequest request, User currentUser) {
        try {
            logger.info("Creating transaction category '{}' by user {}", request.getName(), currentUser.getUsername());

            // Check if category name already exists (case-insensitive)
            if (transactionCategoryRepository.existsByNameIgnoreCase(request.getName())) {
                logger.warn("Transaction category '{}' already exists", request.getName());
                return ApiResponse.error("A category with this name already exists");
            }

            // Create new category
            TransactionCategory category = TransactionCategory.createUserCategory(request.getName());
            TransactionCategory savedCategory = transactionCategoryRepository.save(category);

            logger.info("Transaction category '{}' created successfully with ID {}", savedCategory.getName(), savedCategory.getId());
            return ApiResponse.success("Transaction category created successfully", TransactionCategoryResponse.fromEntity(savedCategory));

        } catch (Exception e) {
            logger.error("Error creating transaction category: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to create transaction category: " + e.getMessage());
        }
    }

    /**
     * Update an existing transaction category
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse updateCategory(Long categoryId, UpdateTransactionCategoryRequest request, User currentUser) {
        try {
            logger.info("Updating transaction category {} by user {}", categoryId, currentUser.getUsername());

            // Find category
            Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                logger.warn("Transaction category {} not found", categoryId);
                return ApiResponse.error("Transaction category not found");
            }

            TransactionCategory category = categoryOpt.get();

            // Check if it's a system default category
            if (category.isSystemCategory()) {
                logger.warn("User {} attempted to update system category {}", currentUser.getUsername(), categoryId);
                return ApiResponse.error("Cannot update system default categories");
            }

            // Check if new name already exists (excluding current category)
            if (transactionCategoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), categoryId)) {
                logger.warn("Transaction category name '{}' already exists", request.getName());
                return ApiResponse.error("A category with this name already exists");
            }

            // Update category
            String oldName = category.getName();
            category.setName(request.getName());
            TransactionCategory savedCategory = transactionCategoryRepository.save(category);

            logger.info("Transaction category {} updated from '{}' to '{}'", categoryId, oldName, savedCategory.getName());
            return ApiResponse.success("Transaction category updated successfully", TransactionCategoryResponse.fromEntity(savedCategory));

        } catch (Exception e) {
            logger.error("Error updating transaction category {}: {}", categoryId, e.getMessage(), e);
            return ApiResponse.error("Failed to update transaction category: " + e.getMessage());
        }
    }

    /**
     * Delete a transaction category
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse deleteCategory(Long categoryId, User currentUser) {
        try {
            logger.info("Deleting transaction category {} by user {}", categoryId, currentUser.getUsername());

            // Find category
            Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                logger.warn("Transaction category {} not found", categoryId);
                return ApiResponse.error("Transaction category not found");
            }

            TransactionCategory category = categoryOpt.get();

            // Check if it's a system default category
            if (category.isSystemCategory()) {
                logger.warn("User {} attempted to delete system category {}", currentUser.getUsername(), categoryId);
                return ApiResponse.error("Cannot delete system default categories");
            }

            // Check if category has transactions
            if (category.hasTransactions()) {
                logger.warn("Cannot delete category {} - it has {} transactions", categoryId, category.getTransactionCount());
                return ApiResponse.error("Cannot delete category that has transactions. Please reassign or delete the transactions first.");
            }

            // Delete category
            String categoryName = category.getName();
            transactionCategoryRepository.delete(category);

            logger.info("Transaction category '{}' deleted successfully", categoryName);
            return ApiResponse.success("Transaction category deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting transaction category {}: {}", categoryId, e.getMessage(), e);
            return ApiResponse.error("Failed to delete transaction category: " + e.getMessage());
        }
    }

    /**
     * Get transaction category by ID
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getCategory(Long categoryId, User currentUser) {
        try {
            logger.debug("Getting transaction category {} for user {}", categoryId, currentUser.getUsername());

            // Find category
            Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                logger.warn("Transaction category {} not found", categoryId);
                return ApiResponse.error("Transaction category not found");
            }

            TransactionCategory category = categoryOpt.get();
            return ApiResponse.success("Transaction category retrieved successfully", TransactionCategoryResponse.fromEntity(category));

        } catch (Exception e) {
            logger.error("Error retrieving transaction category {}: {}", categoryId, e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve transaction category: " + e.getMessage());
        }
    }

    /**
     * Get all transaction categories
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getAllCategories(User currentUser) {
        try {
            logger.debug("Getting all transaction categories for user {}", currentUser.getUsername());

            List<TransactionCategory> categories = transactionCategoryRepository.findAllByOrderByNameAsc();
            List<TransactionCategoryResponse> responses = categories.stream()
                .map(TransactionCategoryResponse::fromEntity)
                .collect(Collectors.toList());

            logger.debug("Retrieved {} transaction categories", responses.size());
            return ApiResponse.success("Transaction categories retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Error retrieving transaction categories: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve transaction categories: " + e.getMessage());
        }
    }

    /**
     * Get system default categories
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getSystemCategories(User currentUser) {
        try {
            logger.debug("Getting system transaction categories for user {}", currentUser.getUsername());

            List<TransactionCategory> categories = transactionCategoryRepository.findByIsSystemDefaultTrueOrderByNameAsc();
            List<TransactionCategoryResponse> responses = categories.stream()
                .map(TransactionCategoryResponse::fromEntity)
                .collect(Collectors.toList());

            logger.debug("Retrieved {} system transaction categories", responses.size());
            return ApiResponse.success("System transaction categories retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Error retrieving system transaction categories: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve system transaction categories: " + e.getMessage());
        }
    }

    /**
     * Get user-created categories
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getUserCategories(User currentUser) {
        try {
            logger.debug("Getting user-created transaction categories for user {}", currentUser.getUsername());

            List<TransactionCategory> categories = transactionCategoryRepository.findByIsSystemDefaultFalseOrderByNameAsc();
            List<TransactionCategoryResponse> responses = categories.stream()
                .map(TransactionCategoryResponse::fromEntity)
                .collect(Collectors.toList());

            logger.debug("Retrieved {} user-created transaction categories", responses.size());
            return ApiResponse.success("User-created transaction categories retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Error retrieving user-created transaction categories: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve user-created transaction categories: " + e.getMessage());
        }
    }

    /**
     * Search categories by name
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse searchCategories(String searchTerm, User currentUser) {
        try {
            logger.debug("Searching transaction categories with term '{}' for user {}", searchTerm, currentUser.getUsername());

            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return getAllCategories(currentUser);
            }

            List<TransactionCategory> categories = transactionCategoryRepository.findByNameContainingIgnoreCase(searchTerm.trim());
            List<TransactionCategoryResponse> responses = categories.stream()
                .map(TransactionCategoryResponse::fromEntity)
                .collect(Collectors.toList());

            logger.debug("Found {} transaction categories matching search term '{}'", responses.size(), searchTerm);
            return ApiResponse.success("Transaction categories search completed", responses);

        } catch (Exception e) {
            logger.error("Error searching transaction categories: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to search transaction categories: " + e.getMessage());
        }
    }

    /**
     * Get categories with transaction counts
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getCategoriesWithCounts(User currentUser) {
        try {
            logger.debug("Getting transaction categories with counts for user {}", currentUser.getUsername());

            List<Object[]> results = transactionCategoryRepository.findCategoriesWithTransactionCount();
            List<TransactionCategoryResponse> responses = results.stream()
                .map(result -> {
                    TransactionCategory category = (TransactionCategory) result[0];
                    Long count = (Long) result[1];
                    return TransactionCategoryResponse.fromEntityWithCount(category, count.intValue());
                })
                .collect(Collectors.toList());

            logger.debug("Retrieved {} transaction categories with counts", responses.size());
            return ApiResponse.success("Transaction categories with counts retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Error retrieving transaction categories with counts: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve transaction categories with counts: " + e.getMessage());
        }
    }

    /**
     * Get categories that can be deleted
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getDeletableCategories(User currentUser) {
        try {
            logger.debug("Getting deletable transaction categories for user {}", currentUser.getUsername());

            List<TransactionCategory> categories = transactionCategoryRepository.findDeletableUserCategories();
            List<TransactionCategoryResponse> responses = categories.stream()
                .map(TransactionCategoryResponse::fromEntity)
                .collect(Collectors.toList());

            logger.debug("Retrieved {} deletable transaction categories", responses.size());
            return ApiResponse.success("Deletable transaction categories retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Error retrieving deletable transaction categories: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve deletable transaction categories: " + e.getMessage());
        }
    }

    /**
     * Get most used categories
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getMostUsedCategories(User currentUser, int limit) {
        try {
            logger.debug("Getting top {} most used transaction categories for user {}", limit, currentUser.getUsername());

            List<Object[]> results = transactionCategoryRepository.findMostUsedCategories();
            List<TransactionCategoryResponse> responses = results.stream()
                .limit(limit)
                .map(result -> {
                    TransactionCategory category = (TransactionCategory) result[0];
                    Long count = (Long) result[1];
                    return TransactionCategoryResponse.fromEntityWithCount(category, count.intValue());
                })
                .collect(Collectors.toList());

            logger.debug("Retrieved {} most used transaction categories", responses.size());
            return ApiResponse.success("Most used transaction categories retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Error retrieving most used transaction categories: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve most used transaction categories: " + e.getMessage());
        }
    }

    /**
     * Get category summary statistics
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getCategorySummary(User currentUser) {
        try {
            logger.debug("Getting category summary statistics for user {}", currentUser.getUsername());

            List<Map<String, Object>> summary = transactionCategoryRepository.findCategorySummary();
            long systemCount = transactionCategoryRepository.countByIsSystemDefaultTrue();
            long userCount = transactionCategoryRepository.countByIsSystemDefaultFalse();

            // Create summary response
            Map<String, Object> statistics = Map.of(
                "totalCategories", systemCount + userCount,
                "systemCategories", systemCount,
                "userCategories", userCount,
                "categories", summary
            );

            logger.debug("Retrieved category summary: {} total, {} system, {} user",
                        systemCount + userCount, systemCount, userCount);
            return ApiResponse.success("Category summary retrieved successfully", statistics);

        } catch (Exception e) {
            logger.error("Error retrieving category summary: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve category summary: " + e.getMessage());
        }
    }

    /**
     * Get default uncategorized category
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getUncategorizedCategory(User currentUser) {
        try {
            logger.debug("Getting uncategorized category for user {}", currentUser.getUsername());

            Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findUncategorizedCategory();
            if (categoryOpt.isEmpty()) {
                logger.warn("Uncategorized category not found");
                return ApiResponse.error("Uncategorized category not found");
            }

            TransactionCategory category = categoryOpt.get();
            return ApiResponse.success("Uncategorized category retrieved successfully", TransactionCategoryResponse.fromEntity(category));

        } catch (Exception e) {
            logger.error("Error retrieving uncategorized category: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve uncategorized category: " + e.getMessage());
        }
    }

    /**
     * Validate if a category can be deleted
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse validateCategoryDeletion(Long categoryId, User currentUser) {
        try {
            logger.debug("Validating deletion of category {} for user {}", categoryId, currentUser.getUsername());

            // Find category
            Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                return ApiResponse.error("Transaction category not found");
            }

            TransactionCategory category = categoryOpt.get();

            // Check if it's a system category
            if (category.isSystemCategory()) {
                return ApiResponse.error("Cannot delete system default categories");
            }

            // Check if it has transactions
            long transactionCount = transactionCategoryRepository.getTransactionCountByCategoryId(categoryId);
            boolean canDelete = transactionCount == 0;

            Map<String, Object> validation = Map.of(
                "canDelete", canDelete,
                "reason", canDelete ? "Category can be safely deleted" : "Category has " + transactionCount + " transactions",
                "transactionCount", transactionCount,
                "isSystemCategory", category.isSystemCategory()
            );

            return ApiResponse.success("Category deletion validation completed", validation);

        } catch (Exception e) {
            logger.error("Error validating category deletion for {}: {}", categoryId, e.getMessage(), e);
            return ApiResponse.error("Failed to validate category deletion: " + e.getMessage());
        }
    }
}