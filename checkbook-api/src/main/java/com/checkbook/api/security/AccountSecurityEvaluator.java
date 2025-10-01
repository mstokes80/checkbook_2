package com.checkbook.api.security;

import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.PermissionType;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.AccountPermissionRepository;
import com.checkbook.api.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Custom security evaluator for account-specific access control
 * Used with @PreAuthorize annotations in service methods
 */
@Component("accountSecurity")
public class AccountSecurityEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(AccountSecurityEvaluator.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountPermissionRepository accountPermissionRepository;

    /**
     * Check if current user is the owner of the specified account
     */
    public boolean isAccountOwner(Authentication authentication, Long accountId) {
        try {
            if (authentication == null || accountId == null) {
                return false;
            }

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return false;
            }

            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return false;
            }

            Account account = accountOpt.get();
            boolean isOwner = account.isOwner(currentUser);

            logger.debug("User '{}' ownership check for account {}: {}",
                        currentUser.getUsername(), accountId, isOwner);

            return isOwner;

        } catch (Exception e) {
            logger.error("Error checking account ownership for accountId {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has any access to the specified account (owner or has permission)
     */
    public boolean hasAccountAccess(Authentication authentication, Long accountId) {
        try {
            if (authentication == null || accountId == null) {
                return false;
            }

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return false;
            }

            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return false;
            }

            Account account = accountOpt.get();

            // Check if user owns the account
            if (account.isOwner(currentUser)) {
                logger.debug("User '{}' has owner access to account {}", currentUser.getUsername(), accountId);
                return true;
            }

            // Check if user has permission to shared account
            if (account.getIsShared()) {
                boolean hasPermission = accountPermissionRepository.existsByAccountIdAndUserId(accountId, currentUser.getId());
                logger.debug("User '{}' permission check for shared account {}: {}",
                            currentUser.getUsername(), accountId, hasPermission);
                return hasPermission;
            }

            logger.debug("User '{}' has no access to account {}", currentUser.getUsername(), accountId);
            return false;

        } catch (Exception e) {
            logger.error("Error checking account access for accountId {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has full access to the specified account (owner or FULL_ACCESS permission)
     */
    public boolean hasAccountFullAccess(Authentication authentication, Long accountId) {
        try {
            if (authentication == null || accountId == null) {
                return false;
            }

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return false;
            }

            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return false;
            }

            Account account = accountOpt.get();

            // Check if user owns the account
            if (account.isOwner(currentUser)) {
                logger.debug("User '{}' has owner (full) access to account {}", currentUser.getUsername(), accountId);
                return true;
            }

            // Check if user has FULL_ACCESS permission to shared account
            if (account.getIsShared()) {
                boolean hasFullAccess = accountPermissionRepository.existsByAccountIdAndUserIdAndPermissionType(
                        accountId, currentUser.getId(), PermissionType.FULL_ACCESS);
                logger.debug("User '{}' full access check for shared account {}: {}",
                            currentUser.getUsername(), accountId, hasFullAccess);
                return hasFullAccess;
            }

            logger.debug("User '{}' has no full access to account {}", currentUser.getUsername(), accountId);
            return false;

        } catch (Exception e) {
            logger.error("Error checking account full access for accountId {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has view-only access to the specified account
     */
    public boolean hasAccountViewAccess(Authentication authentication, Long accountId) {
        try {
            if (authentication == null || accountId == null) {
                return false;
            }

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return false;
            }

            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return false;
            }

            Account account = accountOpt.get();

            // Owner always has view access
            if (account.isOwner(currentUser)) {
                return true;
            }

            // Check if user has any permission to shared account (VIEW_ONLY, TRANSACTION_ONLY, or FULL_ACCESS)
            if (account.getIsShared()) {
                Optional<PermissionType> permission = accountPermissionRepository
                        .findPermissionTypeByAccountIdAndUserId(accountId, currentUser.getId());
                if (permission.isPresent()) {
                    // All permission types include view access
                    return permission.get().canView();
                }
            }

            return false;

        } catch (Exception e) {
            logger.error("Error checking account view access for accountId {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has transaction access to the specified account (TRANSACTION_ONLY or FULL_ACCESS)
     */
    public boolean hasAccountTransactionAccess(Authentication authentication, Long accountId) {
        try {
            if (authentication == null || accountId == null) {
                return false;
            }

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return false;
            }

            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return false;
            }

            Account account = accountOpt.get();

            // Owner always has transaction access
            if (account.isOwner(currentUser)) {
                return true;
            }

            // Check if user has TRANSACTION_ONLY or FULL_ACCESS permission to shared account
            if (account.getIsShared()) {
                Optional<PermissionType> permission = accountPermissionRepository
                        .findPermissionTypeByAccountIdAndUserId(accountId, currentUser.getId());
                if (permission.isPresent()) {
                    return permission.get().canManageTransactions();
                }
            }

            return false;

        } catch (Exception e) {
            logger.error("Error checking account transaction access for accountId {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user can manage permissions for the specified account (must be owner)
     */
    public boolean canManageAccountPermissions(Authentication authentication, Long accountId) {
        try {
            if (authentication == null || accountId == null) {
                logger.debug("canManageAccountPermissions: authentication or accountId is null");
                return false;
            }

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                logger.debug("canManageAccountPermissions: currentUser is null");
                return false;
            }

            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                logger.debug("canManageAccountPermissions: account {} not found", accountId);
                return false;
            }

            Account account = accountOpt.get();

            // Debug account and user info
            logger.debug("canManageAccountPermissions: checking user '{}' (ID: {}) for account {} (Owner ID: {})",
                        currentUser.getUsername(), currentUser.getId(), accountId,
                        account.getOwner() != null ? account.getOwner().getId() : "null");

            // Only owners can manage permissions (regardless of current sharing status)
            // Fix applied: removed requirement for account to be already shared
            boolean canManage = account.isOwner(currentUser);
            logger.debug("User '{}' can manage permissions for account {}: {}",
                        currentUser.getUsername(), accountId, canManage);

            return canManage;

        } catch (Exception e) {
            logger.error("Error checking permission management access for accountId {}: {}", accountId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if current user has access to any account (for listing operations)
     */
    public boolean hasAnyAccountAccess(Authentication authentication) {
        try {
            if (authentication == null) {
                return false;
            }

            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return false;
            }

            // If user is authenticated and has USER role, they can list their accessible accounts
            return authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"));

        } catch (Exception e) {
            logger.error("Error checking any account access: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get current user from authentication
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }

        return null;
    }
}