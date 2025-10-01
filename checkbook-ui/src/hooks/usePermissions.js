import { useMemo } from 'react';
import { permissionIncludes, getPermissionLevel } from '../components/accounts/PermissionBadge';

/**
 * Custom hook for permission management and checks
 */
export const usePermissions = (account, userPermission = null) => {
  return useMemo(() => {
    if (!account) {
      return {
        hasAccess: false,
        isOwner: false,
        currentPermission: null,
        canView: false,
        canAddTransactions: false,
        canModifyAccount: false,
        canManagePermissions: false,
        canRequestUpgrade: false,
        hasPermission: () => false,
        getUpgradeOptions: () => []
      };
    }

    const currentPermission = userPermission || account.userPermission;
    const isOwner = Boolean(account.isOwner);
    const hasAccess = isOwner || Boolean(currentPermission);

    // Permission checks
    const canView = isOwner || (currentPermission && permissionIncludes(currentPermission, 'VIEW_ONLY'));
    const canAddTransactions = isOwner || (currentPermission && permissionIncludes(currentPermission, 'TRANSACTION_ONLY'));
    const canModifyAccount = isOwner || (currentPermission && permissionIncludes(currentPermission, 'FULL_ACCESS'));
    const canManagePermissions = isOwner; // Only owners can manage permissions

    // Can request upgrade if not owner and doesn't have highest permission
    const canRequestUpgrade = !isOwner &&
      currentPermission &&
      getPermissionLevel(currentPermission) < getPermissionLevel('FULL_ACCESS');

    // Generic permission check function
    const hasPermission = (requiredPermission) => {
      if (isOwner) return true;
      if (!currentPermission) return false;
      return permissionIncludes(currentPermission, requiredPermission);
    };

    // Get available upgrade options
    const getUpgradeOptions = () => {
      if (isOwner || !currentPermission) return [];

      const currentLevel = getPermissionLevel(currentPermission);
      const allPermissions = ['VIEW_ONLY', 'TRANSACTION_ONLY', 'FULL_ACCESS'];

      return allPermissions.filter(permission =>
        getPermissionLevel(permission) > currentLevel
      );
    };

    return {
      hasAccess,
      isOwner,
      currentPermission,
      canView,
      canAddTransactions,
      canModifyAccount,
      canManagePermissions,
      canRequestUpgrade,
      hasPermission,
      getUpgradeOptions
    };
  }, [account, userPermission]);
};

/**
 * Hook for permission-based UI state
 */
export const usePermissionUI = (account, userPermission = null) => {
  const permissions = usePermissions(account, userPermission);

  return useMemo(() => {
    const { isOwner, currentPermission, hasPermission } = permissions;

    // Button states for common actions
    const buttonStates = {
      viewDetails: {
        enabled: permissions.canView,
        tooltip: permissions.canView ? '' : 'No access to view account details'
      },
      addTransaction: {
        enabled: permissions.canAddTransactions,
        tooltip: permissions.canAddTransactions ? '' : 'Requires transaction permission or higher'
      },
      editAccount: {
        enabled: permissions.canModifyAccount,
        tooltip: permissions.canModifyAccount ? '' : 'Requires full access permission'
      },
      managePermissions: {
        enabled: permissions.canManagePermissions,
        tooltip: permissions.canManagePermissions ? '' : 'Only account owners can manage permissions'
      },
      requestUpgrade: {
        enabled: permissions.canRequestUpgrade,
        tooltip: permissions.canRequestUpgrade ? '' : 'No upgrades available'
      }
    };

    // CSS classes for permission-based styling
    const cssClasses = {
      permissionIndicator: isOwner
        ? 'text-green-600 bg-green-100'
        : currentPermission
          ? 'text-blue-600 bg-blue-100'
          : 'text-gray-600 bg-gray-100',

      accessLevel: isOwner ? 'owner' : currentPermission?.toLowerCase() || 'no-access',

      restrictedButton: 'opacity-50 cursor-not-allowed',

      enabledButton: 'cursor-pointer hover:bg-opacity-80'
    };

    // Helper to get appropriate button props
    const getButtonProps = (action, baseProps = {}) => {
      const state = buttonStates[action];
      if (!state) return baseProps;

      return {
        ...baseProps,
        disabled: !state.enabled,
        title: state.tooltip || baseProps.title,
        className: `${baseProps.className || ''} ${
          state.enabled ? cssClasses.enabledButton : cssClasses.restrictedButton
        }`.trim()
      };
    };

    return {
      ...permissions,
      buttonStates,
      cssClasses,
      getButtonProps
    };
  }, [permissions]);
};

/**
 * Hook for account permission context
 */
export const useAccountPermissionContext = (accounts = []) => {
  return useMemo(() => {
    const ownedAccounts = accounts.filter(account => account.isOwner);
    const sharedAccounts = accounts.filter(account => !account.isOwner && account.userPermission);

    const permissionSummary = {
      totalAccounts: accounts.length,
      ownedAccounts: ownedAccounts.length,
      sharedAccounts: sharedAccounts.length,
      viewOnlyAccounts: sharedAccounts.filter(acc => acc.userPermission === 'VIEW_ONLY').length,
      transactionAccounts: sharedAccounts.filter(acc => acc.userPermission === 'TRANSACTION_ONLY').length,
      fullAccessAccounts: sharedAccounts.filter(acc => acc.userPermission === 'FULL_ACCESS').length
    };

    const hasAnyManagementAccess = ownedAccounts.length > 0;
    const hasAnySharedAccess = sharedAccounts.length > 0;

    return {
      ownedAccounts,
      sharedAccounts,
      permissionSummary,
      hasAnyManagementAccess,
      hasAnySharedAccess
    };
  }, [accounts]);
};

export default usePermissions;