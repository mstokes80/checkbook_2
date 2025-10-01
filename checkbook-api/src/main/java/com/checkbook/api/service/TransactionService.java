package com.checkbook.api.service;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.Transaction;
import com.checkbook.api.entity.TransactionCategory;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.AccountRepository;
import com.checkbook.api.repository.TransactionRepository;
import com.checkbook.api.repository.TransactionCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PermissionValidationService permissionValidationService;

    /**
     * Create a new transaction
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse createTransaction(CreateTransactionRequest request, User currentUser) {
        try {
            logger.info("Creating transaction for account {} by user {}", request.getAccountId(), currentUser.getUsername());

            // Validate account access and permission
            Optional<Account> accountOpt = accountRepository.findById(request.getAccountId());
            if (accountOpt.isEmpty()) {
                logger.warn("Account {} not found", request.getAccountId());
                return ApiResponse.error("Account not found");
            }

            Account account = accountOpt.get();
            if (!permissionValidationService.hasAccountTransactionAccess(currentUser, account.getId())) {
                logger.warn("User {} does not have transaction access to account {}", currentUser.getUsername(), account.getId());
                return ApiResponse.error("Access denied - you don't have permission to create transactions for this account");
            }

            // Validate category if provided
            TransactionCategory category = null;
            if (request.getCategoryId() != null) {
                Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findById(request.getCategoryId());
                if (categoryOpt.isEmpty()) {
                    logger.warn("Transaction category {} not found", request.getCategoryId());
                    return ApiResponse.error("Transaction category not found");
                }
                category = categoryOpt.get();
            }

            // Create transaction
            Transaction transaction = new Transaction(account, request.getAmount(), request.getDescription(), request.getTransactionDate());
            transaction.setCategory(category);
            transaction.setNotes(request.getNotes());
            transaction.setCreatedBy(currentUser.getUsername());
            transaction.setUpdatedBy(currentUser.getUsername());

            Transaction savedTransaction = transactionRepository.save(transaction);

            // Create audit log entry
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("transactionId", savedTransaction.getId());
            auditDetails.put("amount", savedTransaction.getAmount());
            auditDetails.put("description", savedTransaction.getDescription());
            auditDetails.put("transactionDate", savedTransaction.getTransactionDate().toString());
            if (category != null) {
                auditDetails.put("categoryId", category.getId());
                auditDetails.put("categoryName", category.getName());
            }
            // Add user permission information
            var userPermission = permissionValidationService.getUserPermissionLevel(currentUser, account.getId());
            auditDetails.put("userPermissionLevel", userPermission != null ? userPermission.toString() : "OWNER");

            auditLogService.logTransactionAdded(account.getId(), currentUser, auditDetails);

            logger.info("Transaction {} created successfully for account {}", savedTransaction.getId(), account.getId());
            return ApiResponse.success("Transaction created successfully", TransactionResponse.fromEntity(savedTransaction));

        } catch (Exception e) {
            logger.error("Error creating transaction: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to create transaction: " + e.getMessage());
        }
    }

    /**
     * Update an existing transaction
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse updateTransaction(Long transactionId, UpdateTransactionRequest request, User currentUser) {
        try {
            logger.info("Updating transaction {} by user {}", transactionId, currentUser.getUsername());

            // Find transaction
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                logger.warn("Transaction {} not found", transactionId);
                return ApiResponse.error("Transaction not found");
            }

            Transaction transaction = transactionOpt.get();
            Account account = transaction.getAccount();

            // Validate account access and permission
            if (!permissionValidationService.hasAccountTransactionAccess(currentUser, account.getId())) {
                logger.warn("User {} does not have permission to update transaction {} for account {}",
                           currentUser.getUsername(), transactionId, account.getId());
                return ApiResponse.error("Access denied - you don't have permission to update transactions for this account");
            }

            // Store old values for audit
            Map<String, Object> oldValues = new HashMap<>();
            oldValues.put("amount", transaction.getAmount());
            oldValues.put("description", transaction.getDescription());
            oldValues.put("transactionDate", transaction.getTransactionDate().toString());
            oldValues.put("categoryId", transaction.getCategory() != null ? transaction.getCategory().getId() : null);
            oldValues.put("notes", transaction.getNotes());

            // Validate category if provided
            TransactionCategory category = null;
            if (request.getCategoryId() != null) {
                Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findById(request.getCategoryId());
                if (categoryOpt.isEmpty()) {
                    logger.warn("Transaction category {} not found", request.getCategoryId());
                    return ApiResponse.error("Transaction category not found");
                }
                category = categoryOpt.get();
            }

            // Update transaction
            transaction.setAmount(request.getAmount());
            transaction.setDescription(request.getDescription());
            transaction.setTransactionDate(request.getTransactionDate());
            transaction.setCategory(category);
            transaction.setNotes(request.getNotes());
            transaction.setUpdatedBy(currentUser.getUsername());

            Transaction savedTransaction = transactionRepository.save(transaction);

            // Create audit log entry
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("transactionId", savedTransaction.getId());
            auditDetails.put("oldValues", oldValues);
            auditDetails.put("newAmount", savedTransaction.getAmount());
            auditDetails.put("newDescription", savedTransaction.getDescription());
            auditDetails.put("newTransactionDate", savedTransaction.getTransactionDate().toString());
            if (category != null) {
                auditDetails.put("newCategoryId", category.getId());
                auditDetails.put("newCategoryName", category.getName());
            }
            // Add user permission information
            var userPermission = permissionValidationService.getUserPermissionLevel(currentUser, account.getId());
            auditDetails.put("userPermissionLevel", userPermission != null ? userPermission.toString() : "OWNER");

            auditLogService.logTransactionModified(account.getId(), currentUser, auditDetails);

            logger.info("Transaction {} updated successfully", savedTransaction.getId());
            return ApiResponse.success("Transaction updated successfully", TransactionResponse.fromEntity(savedTransaction));

        } catch (Exception e) {
            logger.error("Error updating transaction {}: {}", transactionId, e.getMessage(), e);
            return ApiResponse.error("Failed to update transaction: " + e.getMessage());
        }
    }

    /**
     * Delete a transaction
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse deleteTransaction(Long transactionId, User currentUser) {
        try {
            logger.info("Deleting transaction {} by user {}", transactionId, currentUser.getUsername());

            // Find transaction
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                logger.warn("Transaction {} not found", transactionId);
                return ApiResponse.error("Transaction not found");
            }

            Transaction transaction = transactionOpt.get();
            Account account = transaction.getAccount();

            // Validate account access and permission
            if (!permissionValidationService.hasAccountTransactionAccess(currentUser, account.getId())) {
                logger.warn("User {} does not have permission to delete transaction {} for account {}",
                           currentUser.getUsername(), transactionId, account.getId());
                return ApiResponse.error("Access denied - you don't have permission to delete transactions for this account");
            }

            // Store transaction details for audit before deletion
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("transactionId", transaction.getId());
            auditDetails.put("amount", transaction.getAmount());
            auditDetails.put("description", transaction.getDescription());
            auditDetails.put("transactionDate", transaction.getTransactionDate().toString());
            if (transaction.getCategory() != null) {
                auditDetails.put("categoryId", transaction.getCategory().getId());
                auditDetails.put("categoryName", transaction.getCategory().getName());
            }
            // Add user permission information
            var userPermission = permissionValidationService.getUserPermissionLevel(currentUser, account.getId());
            auditDetails.put("userPermissionLevel", userPermission != null ? userPermission.toString() : "OWNER");

            // Delete transaction
            transactionRepository.delete(transaction);

            // Create audit log entry
            auditLogService.logTransactionDeleted(account.getId(), currentUser, auditDetails);

            logger.info("Transaction {} deleted successfully", transactionId);
            return ApiResponse.success("Transaction deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting transaction {}: {}", transactionId, e.getMessage(), e);
            return ApiResponse.error("Failed to delete transaction: " + e.getMessage());
        }
    }

    /**
     * Get transaction by ID
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getTransaction(Long transactionId, User currentUser) {
        try {
            logger.debug("Getting transaction {} for user {}", transactionId, currentUser.getUsername());

            // Find transaction
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                logger.warn("Transaction {} not found", transactionId);
                return ApiResponse.error("Transaction not found");
            }

            Transaction transaction = transactionOpt.get();
            Account account = transaction.getAccount();

            // Validate account access
            if (!permissionValidationService.hasAccountAccess(currentUser, account.getId())) {
                logger.warn("User {} does not have access to view transaction {} for account {}",
                           currentUser.getUsername(), transactionId, account.getId());
                return ApiResponse.error("Access denied - you don't have permission to view transactions for this account");
            }

            return ApiResponse.success("Transaction retrieved successfully", TransactionResponse.fromEntity(transaction));

        } catch (Exception e) {
            logger.error("Error retrieving transaction {}: {}", transactionId, e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve transaction: " + e.getMessage());
        }
    }

    /**
     * Get transactions for an account with optional filtering
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getAccountTransactions(Long accountId, Long categoryId, LocalDate startDate, LocalDate endDate,
                                            String searchTerm, User currentUser, Pageable pageable) {
        try {
            logger.debug("Getting transactions for account {} by user {}", accountId, currentUser.getUsername());

            // Validate account access
            if (!permissionValidationService.hasAccountAccess(currentUser, accountId)) {
                logger.warn("User {} does not have access to account {}", currentUser.getUsername(), accountId);
                return ApiResponse.error("Access denied - you don't have permission to view transactions for this account");
            }

            Page<Transaction> transactionsPage;

            // Apply filters based on provided parameters
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                transactionsPage = transactionRepository.findByAccountIdAndDescriptionContainingIgnoreCase(
                    accountId, searchTerm.trim(), pageable);
            } else if (categoryId != null && startDate != null && endDate != null) {
                transactionsPage = transactionRepository.findByAccountIdAndCategoryIdAndTransactionDateBetween(
                    accountId, categoryId, startDate, endDate, pageable);
            } else if (categoryId != null) {
                transactionsPage = transactionRepository.findByAccountIdAndCategoryId(accountId, categoryId, pageable);
            } else if (startDate != null && endDate != null) {
                transactionsPage = transactionRepository.findByAccountIdAndTransactionDateBetween(
                    accountId, startDate, endDate, pageable);
            } else {
                transactionsPage = transactionRepository.findByAccountIdOrderByTransactionDateDescIdDesc(accountId, pageable);
            }

            // Convert to response DTOs
            Page<TransactionResponse> responsePage = transactionsPage.map(TransactionResponse::fromEntity);

            logger.debug("Retrieved {} transactions for account {} (page {}/{})",
                        responsePage.getNumberOfElements(), accountId,
                        responsePage.getNumber() + 1, responsePage.getTotalPages());

            return ApiResponse.success("Transactions retrieved successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error retrieving transactions for account {}: {}", accountId, e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve transactions: " + e.getMessage());
        }
    }

    /**
     * Get recent transactions for an account
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getRecentTransactions(Long accountId, int limit, User currentUser) {
        try {
            logger.debug("Getting recent {} transactions for account {} by user {}", limit, accountId, currentUser.getUsername());

            // Validate account access
            if (!permissionValidationService.hasAccountAccess(currentUser, accountId)) {
                logger.warn("User {} does not have access to account {}", currentUser.getUsername(), accountId);
                return ApiResponse.error("Access denied - you don't have permission to view transactions for this account");
            }

            Pageable pageable = PageRequest.of(0, limit);
            List<Transaction> transactions = transactionRepository.findRecentByAccountId(accountId, pageable);

            List<TransactionResponse> responses = transactions.stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

            logger.debug("Retrieved {} recent transactions for account {}", responses.size(), accountId);
            return ApiResponse.success("Recent transactions retrieved successfully", responses);

        } catch (Exception e) {
            logger.error("Error retrieving recent transactions for account {}: {}", accountId, e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve recent transactions: " + e.getMessage());
        }
    }

    /**
     * Get transaction statistics for an account
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getTransactionStatistics(Long accountId, LocalDate startDate, LocalDate endDate, User currentUser) {
        try {
            logger.debug("Getting transaction statistics for account {} from {} to {} by user {}",
                        accountId, startDate, endDate, currentUser.getUsername());

            // Validate account access
            if (!permissionValidationService.hasAccountAccess(currentUser, accountId)) {
                logger.warn("User {} does not have access to account {}", currentUser.getUsername(), accountId);
                return ApiResponse.error("Access denied - you don't have permission to view statistics for this account");
            }

            // Calculate statistics
            long totalCount = transactionRepository.countByAccountIdAndDateRange(accountId, startDate, endDate);
            BigDecimal totalAmount = transactionRepository.sumAmountByAccountIdAndDateRange(accountId, startDate, endDate);
            BigDecimal totalCredits = transactionRepository.sumCreditsInDateRange(accountId, startDate, endDate);
            BigDecimal totalDebits = transactionRepository.sumDebitsInDateRange(accountId, startDate, endDate);
            BigDecimal averageAmount = transactionRepository.getAverageAmountByAccountIdAndDateRange(accountId, startDate, endDate);

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("accountId", accountId);
            statistics.put("startDate", startDate);
            statistics.put("endDate", endDate);
            statistics.put("totalTransactions", totalCount);
            statistics.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);
            statistics.put("totalCredits", totalCredits != null ? totalCredits : BigDecimal.ZERO);
            statistics.put("totalDebits", totalDebits != null ? totalDebits.abs() : BigDecimal.ZERO);
            statistics.put("averageAmount", averageAmount != null ? averageAmount : BigDecimal.ZERO);

            logger.debug("Retrieved transaction statistics for account {}: {} transactions", accountId, totalCount);
            return ApiResponse.success("Transaction statistics retrieved successfully", statistics);

        } catch (Exception e) {
            logger.error("Error retrieving transaction statistics for account {}: {}", accountId, e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve transaction statistics: " + e.getMessage());
        }
    }

    /**
     * Get uncategorized transactions for an account
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getUncategorizedTransactions(Long accountId, User currentUser, Pageable pageable) {
        try {
            logger.debug("Getting uncategorized transactions for account {} by user {}", accountId, currentUser.getUsername());

            // Validate account access
            if (!permissionValidationService.hasAccountAccess(currentUser, accountId)) {
                logger.warn("User {} does not have access to account {}", currentUser.getUsername(), accountId);
                return ApiResponse.error("Access denied - you don't have permission to view transactions for this account");
            }

            Page<Transaction> transactionsPage = transactionRepository.findUncategorizedByAccountId(accountId, pageable);
            Page<TransactionResponse> responsePage = transactionsPage.map(TransactionResponse::fromEntity);

            logger.debug("Retrieved {} uncategorized transactions for account {}",
                        responsePage.getNumberOfElements(), accountId);

            return ApiResponse.success("Uncategorized transactions retrieved successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error retrieving uncategorized transactions for account {}: {}", accountId, e.getMessage(), e);
            return ApiResponse.error("Failed to retrieve uncategorized transactions: " + e.getMessage());
        }
    }

    /**
     * Bulk categorize transactions
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse bulkCategorizeTransactions(List<Long> transactionIds, Long categoryId, User currentUser) {
        try {
            logger.info("Bulk categorizing {} transactions to category {} by user {}",
                       transactionIds.size(), categoryId, currentUser.getUsername());

            // Validate category
            Optional<TransactionCategory> categoryOpt = transactionCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                logger.warn("Transaction category {} not found", categoryId);
                return ApiResponse.error("Transaction category not found");
            }
            TransactionCategory category = categoryOpt.get();

            int updatedCount = 0;
            for (Long transactionId : transactionIds) {
                Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
                if (transactionOpt.isPresent()) {
                    Transaction transaction = transactionOpt.get();

                    // Validate access for each transaction
                    if (permissionValidationService.hasAccountTransactionAccess(currentUser, transaction.getAccount().getId())) {
                        transaction.setCategory(category);
                        transaction.setUpdatedBy(currentUser.getUsername());
                        transactionRepository.save(transaction);
                        updatedCount++;
                    }
                }
            }

            logger.info("Successfully categorized {} out of {} transactions", updatedCount, transactionIds.size());
            return ApiResponse.success(String.format("Successfully categorized %d out of %d transactions",
                                     updatedCount, transactionIds.size()));

        } catch (Exception e) {
            logger.error("Error bulk categorizing transactions: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to categorize transactions: " + e.getMessage());
        }
    }
}