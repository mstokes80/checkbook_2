package com.checkbook.api.service;

import com.checkbook.api.entity.Account;
import com.checkbook.api.entity.PermissionType;
import com.checkbook.api.entity.User;
import com.checkbook.api.repository.AccountPermissionRepository;
import com.checkbook.api.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PermissionValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionValidationService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountPermissionRepository accountPermissionRepository;

    public boolean hasAccountAccess(User user, Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (!accountOpt.isPresent()) {
            logger.debug("Account {} not found", accountId);
            return false;
        }

        Account account = accountOpt.get();

        if (account.isOwner(user)) {
            logger.debug("User '{}' has access to account {} (owner)", user.getUsername(), accountId);
            return true;
        }

        if (account.getIsShared()) {
            boolean hasPermission = accountPermissionRepository.existsByAccountIdAndUserId(accountId, user.getId());
            logger.debug("User '{}' has access to shared account {}: {}", user.getUsername(), accountId, hasPermission);
            return hasPermission;
        }

        logger.debug("User '{}' does not have access to account {}", user.getUsername(), accountId);
        return false;
    }

    public boolean hasAccountViewAccess(User user, Long accountId) {
        return hasMinimumPermission(user, accountId, PermissionType.VIEW_ONLY);
    }

    public boolean hasAccountTransactionAccess(User user, Long accountId) {
        return hasMinimumPermission(user, accountId, PermissionType.TRANSACTION_ONLY);
    }

    public boolean hasAccountFullAccess(User user, Long accountId) {
        return hasMinimumPermission(user, accountId, PermissionType.FULL_ACCESS);
    }

    public boolean isAccountOwner(User user, Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (!accountOpt.isPresent()) {
            logger.debug("Account {} not found", accountId);
            return false;
        }

        Account account = accountOpt.get();
        boolean isOwner = account.isOwner(user);
        logger.debug("User '{}' is owner of account {}: {}", user.getUsername(), accountId, isOwner);
        return isOwner;
    }

    public boolean canManageAccountPermissions(User user, Long accountId) {
        return isAccountOwner(user, accountId);
    }

    public PermissionType getUserPermissionLevel(User user, Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (!accountOpt.isPresent()) {
            logger.debug("Account {} not found", accountId);
            return null;
        }

        Account account = accountOpt.get();

        if (account.isOwner(user)) {
            logger.debug("User '{}' has FULL_ACCESS to account {} (owner)", user.getUsername(), accountId);
            return PermissionType.FULL_ACCESS;
        }

        if (account.getIsShared()) {
            Optional<PermissionType> permission = accountPermissionRepository
                    .findPermissionTypeByAccountIdAndUserId(accountId, user.getId());
            if (permission.isPresent()) {
                logger.debug("User '{}' has {} permission to account {}", user.getUsername(), permission.get(), accountId);
                return permission.get();
            }
        }

        logger.debug("User '{}' has no permission to account {}", user.getUsername(), accountId);
        return null;
    }

    public boolean hasMinimumPermission(User user, Long accountId, PermissionType requiredPermission) {
        PermissionType userPermission = getUserPermissionLevel(user, accountId);

        if (userPermission == null) {
            logger.debug("User '{}' has no permission to account {}", user.getUsername(), accountId);
            return false;
        }

        boolean hasMinimum = userPermission.includes(requiredPermission);
        logger.debug("User '{}' has {} permission for account {} - required {}: {}",
                    user.getUsername(), userPermission, accountId, requiredPermission, hasMinimum);
        return hasMinimum;
    }

    public boolean canUpgradePermission(User user, Long accountId, PermissionType fromPermission, PermissionType toPermission) {
        if (!isAccountOwner(user, accountId)) {
            logger.debug("User '{}' cannot upgrade permissions - not account owner for {}", user.getUsername(), accountId);
            return false;
        }

        if (fromPermission != null && fromPermission.includes(toPermission)) {
            logger.debug("Cannot downgrade permission from {} to {}", fromPermission, toPermission);
            return false;
        }

        logger.debug("User '{}' can upgrade permission from {} to {} for account {}",
                    user.getUsername(), fromPermission, toPermission, accountId);
        return true;
    }

    public boolean canRequestPermission(User user, Long accountId, PermissionType requestedPermission) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (!accountOpt.isPresent()) {
            logger.debug("Account {} not found", accountId);
            return false;
        }

        Account account = accountOpt.get();

        if (account.isOwner(user)) {
            logger.debug("User '{}' cannot request permission for own account {}", user.getUsername(), accountId);
            return false;
        }

        if (!account.getIsShared()) {
            logger.debug("Cannot request permission for non-shared account {}", accountId);
            return false;
        }

        PermissionType currentPermission = getUserPermissionLevel(user, accountId);
        if (currentPermission != null && currentPermission.includes(requestedPermission)) {
            logger.debug("User '{}' already has {} permission or higher for account {}",
                        user.getUsername(), requestedPermission, accountId);
            return false;
        }

        logger.debug("User '{}' can request {} permission for account {}",
                    user.getUsername(), requestedPermission, accountId);
        return true;
    }

    public boolean isValidPermissionHierarchy(PermissionType permission) {
        return permission == PermissionType.VIEW_ONLY ||
               permission == PermissionType.TRANSACTION_ONLY ||
               permission == PermissionType.FULL_ACCESS;
    }

    public PermissionType getNextPermissionLevel(PermissionType currentPermission) {
        if (currentPermission == null || currentPermission == PermissionType.VIEW_ONLY) {
            return PermissionType.TRANSACTION_ONLY;
        }
        if (currentPermission == PermissionType.TRANSACTION_ONLY) {
            return PermissionType.FULL_ACCESS;
        }
        return null;
    }

    public PermissionType getPreviousPermissionLevel(PermissionType currentPermission) {
        if (currentPermission == PermissionType.FULL_ACCESS) {
            return PermissionType.TRANSACTION_ONLY;
        }
        if (currentPermission == PermissionType.TRANSACTION_ONLY) {
            return PermissionType.VIEW_ONLY;
        }
        return null;
    }

    public String getPermissionDescription(PermissionType permission) {
        if (permission == null) {
            return "No permission";
        }

        switch (permission) {
            case VIEW_ONLY:
                return "Can view account details and transaction history";
            case TRANSACTION_ONLY:
                return "Can view account details and add transactions";
            case FULL_ACCESS:
                return "Full access including account modification and balance updates";
            default:
                return "Unknown permission level";
        }
    }

    public boolean canPerformAction(User user, Long accountId, String action) {
        switch (action.toUpperCase()) {
            case "VIEW":
            case "READ":
                return hasAccountViewAccess(user, accountId);

            case "ADD_TRANSACTION":
            case "TRANSACTION":
                return hasAccountTransactionAccess(user, accountId);

            case "MODIFY":
            case "UPDATE":
            case "DELETE":
            case "BALANCE_UPDATE":
                return hasAccountFullAccess(user, accountId);

            case "MANAGE_PERMISSIONS":
                return canManageAccountPermissions(user, accountId);

            default:
                logger.warn("Unknown action '{}' for permission check", action);
                return false;
        }
    }

    public boolean validatePermissionRequest(User requester, Long accountId, PermissionType requestedPermission, PermissionType currentPermission) {
        if (!canRequestPermission(requester, accountId, requestedPermission)) {
            return false;
        }

        if (!isValidPermissionHierarchy(requestedPermission)) {
            logger.debug("Invalid permission hierarchy: {}", requestedPermission);
            return false;
        }

        if (currentPermission != null && requestedPermission.getLevel() <= currentPermission.getLevel()) {
            logger.debug("Cannot request permission {} when current permission {} is same or higher",
                        requestedPermission, currentPermission);
            return false;
        }

        return true;
    }
}