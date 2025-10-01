package com.checkbook.api.service;

import com.checkbook.api.dto.*;
import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.AccountPermission;
import com.checkbook.api.entity.PermissionType;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.AccountPermissionRepository;
import com.checkbook.api.repository.AccountRepository;
import com.checkbook.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountPermissionRepository accountPermissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PermissionValidationService permissionValidationService;

    /**
     * Create a new account
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse createAccount(CreateAccountRequest request, User currentUser) {
        try {
            // Check if account name already exists for this user
            if (accountRepository.existsByOwnerIdAndNameIgnoreCase(currentUser.getId(), request.getName())) {
                return ApiResponse.error("Account name already exists");
            }

            // Create new account
            Account account = new Account();
            account.setName(request.getName());
            account.setDescription(request.getDescription());
            account.setAccountType(request.getAccountType());
            account.setBankName(request.getBankName());
            account.setAccountNumberMasked(request.getAccountNumberMasked());
            account.setIsShared(request.getIsShared());
            account.setOwner(currentUser);
            account.setCurrentBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);

            Account savedAccount = accountRepository.save(account);
            logger.info("Created new account '{}' for user '{}'", savedAccount.getName(), currentUser.getUsername());

            return ApiResponse.success("Account created successfully", convertToAccountResponse(savedAccount, currentUser));

        } catch (Exception e) {
            logger.error("Error creating account for user '{}': {}", currentUser.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to create account: " + e.getMessage());
        }
    }

    /**
     * Update an existing account
     */
    @PreAuthorize("hasRole('USER') and @accountSecurity.isAccountOwner(authentication, #accountId)")
    public ApiResponse updateAccount(Long accountId, UpdateAccountRequest request, User currentUser) {
        try {
            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, currentUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found or access denied");
            }

            Account account = accountOpt.get();

            // Check if new name conflicts with existing accounts (excluding current)
            if (!account.getName().equalsIgnoreCase(request.getName()) &&
                accountRepository.existsByOwnerIdAndNameIgnoreCaseAndIdNot(currentUser.getId(), request.getName(), accountId)) {
                return ApiResponse.error("Account name already exists");
            }

            // Update account fields
            account.setName(request.getName());
            account.setDescription(request.getDescription());
            account.setAccountType(request.getAccountType());
            account.setBankName(request.getBankName());
            account.setAccountNumberMasked(request.getAccountNumberMasked());
            account.setIsShared(request.getIsShared());

            Account updatedAccount = accountRepository.save(account);
            logger.info("Updated account '{}' for user '{}'", updatedAccount.getName(), currentUser.getUsername());

            return ApiResponse.success("Account updated successfully", convertToAccountResponse(updatedAccount, currentUser));

        } catch (Exception e) {
            logger.error("Error updating account {} for user '{}': {}", accountId, currentUser.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to update account: " + e.getMessage());
        }
    }

    /**
     * Delete an account
     */
    @PreAuthorize("hasRole('USER') and @accountSecurity.isAccountOwner(authentication, #accountId)")
    public ApiResponse deleteAccount(Long accountId, User currentUser) {
        try {
            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, currentUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found or access denied");
            }

            Account account = accountOpt.get();

            // Delete all permissions for this account first
            accountPermissionRepository.deleteByAccountId(accountId);

            // Delete the account
            accountRepository.delete(account);
            logger.info("Deleted account '{}' for user '{}'", account.getName(), currentUser.getUsername());

            return ApiResponse.success("Account deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting account {} for user '{}': {}", accountId, currentUser.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to delete account: " + e.getMessage());
        }
    }

    /**
     * Get all accounts accessible by user (owned + shared)
     */
    @PreAuthorize("hasRole('USER')")
    public ApiResponse getAccessibleAccounts(User currentUser) {
        try {
            List<Account> accounts = accountRepository.findAccessibleAccountsByUserId(currentUser.getId());
            List<AccountResponse> accountResponses = accounts.stream()
                    .map(account -> convertToAccountResponse(account, currentUser))
                    .collect(Collectors.toList());

            return ApiResponse.success("Accounts retrieved successfully", accountResponses);

        } catch (Exception e) {
            logger.error("Error retrieving accounts for user '{}': {}", currentUser.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to retrieve accounts: " + e.getMessage());
        }
    }

    /**
     * Get account by ID (if user has access)
     */
    @PreAuthorize("hasRole('USER') and @accountSecurity.hasAccountAccess(authentication, #accountId)")
    public ApiResponse getAccountById(Long accountId, User currentUser) {
        try {
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found");
            }

            Account account = accountOpt.get();

            // Check if user has access to this account
            if (!hasUserAccessToAccount(account, currentUser)) {
                return ApiResponse.error("Access denied");
            }

            return ApiResponse.success("Account retrieved successfully", convertToAccountResponse(account, currentUser));

        } catch (Exception e) {
            logger.error("Error retrieving account {} for user '{}': {}", accountId, currentUser.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to retrieve account: " + e.getMessage());
        }
    }

    /**
     * Grant permission to user for shared account
     */
    @PreAuthorize("hasRole('USER') and @accountSecurity.canManageAccountPermissions(authentication, #accountId)")
    public ApiResponse grantPermission(Long accountId, AccountPermissionRequest request, User currentUser) {
        try {
            // Check if account exists and is owned by current user
            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, currentUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found or you don't own this account");
            }

            Account account = accountOpt.get();

            // Check if account is shared
            if (!account.getIsShared()) {
                account.setIsShared(true);
                accountRepository.save(account);
            }

            // Find the user to grant permission to
            Optional<User> targetUserOpt = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail());
            if (!targetUserOpt.isPresent()) {
                return ApiResponse.error("User not found");
            }

            User targetUser = targetUserOpt.get();

            // Check if user is trying to grant permission to themselves
            if (targetUser.getId().equals(currentUser.getId())) {
                return ApiResponse.error("Cannot grant permission to yourself");
            }

            // Check if permission already exists
            Optional<AccountPermission> existingPermission = accountPermissionRepository.findByAccountIdAndUserId(accountId, targetUser.getId());
            if (existingPermission.isPresent()) {
                // Update existing permission
                AccountPermission permission = existingPermission.get();
                PermissionType oldPermission = permission.getPermissionType();
                permission.setPermissionType(request.getPermissionType());
                accountPermissionRepository.save(permission);

                // Audit log permission modification
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("accountName", account.getName());
                auditLogService.logPermissionModified(accountId, currentUser, targetUser,
                                                     oldPermission.name(), request.getPermissionType().name(), auditDetails);

                logger.info("Updated permission for user '{}' on account '{}' to '{}'",
                           targetUser.getUsername(), account.getName(), request.getPermissionType());
                return ApiResponse.success("Permission updated successfully", convertToPermissionResponse(permission));
            } else {
                // Create new permission
                AccountPermission permission = new AccountPermission(account, targetUser, request.getPermissionType());
                AccountPermission savedPermission = accountPermissionRepository.save(permission);

                // Audit log permission granted
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("accountName", account.getName());
                auditLogService.logPermissionGranted(accountId, currentUser, targetUser,
                                                    request.getPermissionType().name(), auditDetails);

                logger.info("Granted '{}' permission to user '{}' for account '{}'",
                           request.getPermissionType(), targetUser.getUsername(), account.getName());
                return ApiResponse.success("Permission granted successfully", convertToPermissionResponse(savedPermission));
            }

        } catch (Exception e) {
            logger.error("Error granting permission for account {} to user '{}': {}", accountId, request.getUsernameOrEmail(), e.getMessage());
            return ApiResponse.error("Failed to grant permission: " + e.getMessage());
        }
    }

    /**
     * Revoke permission from user for shared account
     */
    @PreAuthorize("hasRole('USER') and @accountSecurity.canManageAccountPermissions(authentication, #accountId)")
    public ApiResponse revokePermission(Long accountId, Long userId, User currentUser) {
        try {
            // Check if account exists and is owned by current user
            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, currentUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found or you don't own this account");
            }

            Account account = accountOpt.get();

            // Find and delete the permission
            Optional<AccountPermission> permissionOpt = accountPermissionRepository.findByAccountIdAndUserId(accountId, userId);
            if (!permissionOpt.isPresent()) {
                return ApiResponse.error("Permission not found");
            }

            AccountPermission permission = permissionOpt.get();
            User targetUser = permission.getUser();
            PermissionType revokedPermission = permission.getPermissionType();

            accountPermissionRepository.delete(permission);

            // Audit log permission revoked
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("accountName", account.getName());
            auditLogService.logPermissionRevoked(accountId, currentUser, targetUser,
                                                revokedPermission.name(), auditDetails);

            logger.info("Revoked permission for user '{}' on account '{}'",
                       targetUser.getUsername(), account.getName());

            return ApiResponse.success("Permission revoked successfully");

        } catch (Exception e) {
            logger.error("Error revoking permission for account {} from user {}: {}", accountId, userId, e.getMessage());
            return ApiResponse.error("Failed to revoke permission: " + e.getMessage());
        }
    }

    /**
     * Get all permissions for an account
     */
    @PreAuthorize("hasRole('USER') and @accountSecurity.isAccountOwner(authentication, #accountId)")
    public ApiResponse getAccountPermissions(Long accountId, User currentUser) {
        try {
            // Check if account exists and is owned by current user
            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, currentUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found or you don't own this account");
            }

            List<AccountPermission> permissions = accountPermissionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
            List<AccountPermissionResponse> permissionResponses = permissions.stream()
                    .map(this::convertToPermissionResponse)
                    .collect(Collectors.toList());

            return ApiResponse.success("Permissions retrieved successfully", permissionResponses);

        } catch (Exception e) {
            logger.error("Error retrieving permissions for account {}: {}", accountId, e.getMessage());
            return ApiResponse.error("Failed to retrieve permissions: " + e.getMessage());
        }
    }

    /**
     * Update account balance
     */
    @PreAuthorize("hasRole('USER') and @accountSecurity.hasAccountFullAccess(authentication, #accountId)")
    public ApiResponse updateAccountBalance(Long accountId, BigDecimal newBalance, User currentUser) {
        try {
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found");
            }

            Account account = accountOpt.get();

            // Check if user has full access to this account
            if (!hasUserFullAccessToAccount(account, currentUser)) {
                return ApiResponse.error("Access denied - full access required");
            }

            account.updateBalance(newBalance);
            Account updatedAccount = accountRepository.save(account);
            logger.info("Updated balance for account '{}' to {}", account.getName(), newBalance);

            return ApiResponse.success("Balance updated successfully", convertToAccountResponse(updatedAccount, currentUser));

        } catch (Exception e) {
            logger.error("Error updating balance for account {}: {}", accountId, e.getMessage());
            return ApiResponse.error("Failed to update balance: " + e.getMessage());
        }
    }

    // Helper methods

    private boolean hasUserAccessToAccount(Account account, User user) {
        // User owns the account
        if (account.isOwner(user)) {
            return true;
        }
        // User has permission to shared account
        if (account.getIsShared()) {
            return accountPermissionRepository.existsByAccountIdAndUserId(account.getId(), user.getId());
        }
        return false;
    }

    private boolean hasUserFullAccessToAccount(Account account, User user) {
        // User owns the account
        if (account.isOwner(user)) {
            return true;
        }
        // User has FULL_ACCESS permission to shared account
        if (account.getIsShared()) {
            return accountPermissionRepository.existsByAccountIdAndUserIdAndPermissionType(
                    account.getId(), user.getId(), PermissionType.FULL_ACCESS);
        }
        return false;
    }

    private PermissionType getUserPermissionForAccount(Account account, User user) {
        if (account.isOwner(user)) {
            return PermissionType.FULL_ACCESS; // Owner has full access
        }
        if (account.getIsShared()) {
            Optional<PermissionType> permission = accountPermissionRepository
                    .findPermissionTypeByAccountIdAndUserId(account.getId(), user.getId());
            return permission.orElse(null);
        }
        return null;
    }

    private AccountResponse convertToAccountResponse(Account account, User currentUser) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setDescription(account.getDescription());
        response.setAccountType(account.getAccountType());
        response.setBankName(account.getBankName());
        response.setAccountNumberMasked(account.getAccountNumberMasked());
        response.setIsShared(account.getIsShared());
        response.setCurrentBalance(account.getCurrentBalance());
        response.setDisplayName(account.getDisplayName());
        response.setMaskedAccountInfo(account.getMaskedAccountInfo());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());

        // Set permission information
        response.setIsOwner(account.isOwner(currentUser));
        response.setUserPermission(getUserPermissionForAccount(account, currentUser));
        response.setOwnerId(account.getOwner().getId());
        response.setOwnerName(account.getOwner().getFirstName() + " " + account.getOwner().getLastName());

        return response;
    }

    private AccountPermissionResponse convertToPermissionResponse(AccountPermission permission) {
        AccountPermissionResponse response = new AccountPermissionResponse();
        response.setId(permission.getId());
        response.setAccountId(permission.getAccount().getId());
        response.setAccountName(permission.getAccount().getName());
        response.setUserId(permission.getUser().getId());
        response.setUsername(permission.getUser().getUsername());
        response.setEmail(permission.getUser().getEmail());
        response.setFullName(permission.getUser().getFirstName() + " " + permission.getUser().getLastName());
        response.setPermissionType(permission.getPermissionType());
        response.setCreatedAt(permission.getCreatedAt());
        response.setUpdatedAt(permission.getUpdatedAt());
        return response;
    }
}