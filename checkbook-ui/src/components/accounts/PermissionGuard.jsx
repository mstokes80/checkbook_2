import React from 'react';
import { permissionIncludes } from './PermissionBadge';

/**
 * PermissionGuard component - conditionally renders children based on permission requirements
 */
const PermissionGuard = ({
  account,
  requiredPermission,
  userPermission,
  requireOwnership = false,
  fallback = null,
  children
}) => {
  // If account is not provided, don't render
  if (!account) {
    return fallback;
  }

  // Check ownership requirement
  if (requireOwnership && !account.isOwner) {
    return fallback;
  }

  // If no permission requirement specified, check if user has any access
  if (!requiredPermission) {
    const hasAccess = account.isOwner || userPermission || account.userPermission;
    return hasAccess ? children : fallback;
  }

  // Use provided userPermission or account's userPermission
  const currentPermission = userPermission || account.userPermission;

  // Owner always has access (unless restricted by requireOwnership check above)
  if (account.isOwner) {
    return children;
  }

  // Check if current permission includes required permission
  if (currentPermission && permissionIncludes(currentPermission, requiredPermission)) {
    return children;
  }

  return fallback;
};

/**
 * OwnerOnlyGuard - shorthand for ownership-only content
 */
export const OwnerOnlyGuard = ({ account, fallback = null, children }) => (
  <PermissionGuard
    account={account}
    requireOwnership={true}
    fallback={fallback}
  >
    {children}
  </PermissionGuard>
);

/**
 * ViewPermissionGuard - requires at least VIEW_ONLY permission
 */
export const ViewPermissionGuard = ({ account, userPermission, fallback = null, children }) => (
  <PermissionGuard
    account={account}
    requiredPermission="VIEW_ONLY"
    userPermission={userPermission}
    fallback={fallback}
  >
    {children}
  </PermissionGuard>
);

/**
 * TransactionPermissionGuard - requires at least TRANSACTION_ONLY permission
 */
export const TransactionPermissionGuard = ({ account, userPermission, fallback = null, children }) => (
  <PermissionGuard
    account={account}
    requiredPermission="TRANSACTION_ONLY"
    userPermission={userPermission}
    fallback={fallback}
  >
    {children}
  </PermissionGuard>
);

/**
 * FullAccessGuard - requires FULL_ACCESS permission
 */
export const FullAccessGuard = ({ account, userPermission, fallback = null, children }) => (
  <PermissionGuard
    account={account}
    requiredPermission="FULL_ACCESS"
    userPermission={userPermission}
    fallback={fallback}
  >
    {children}
  </PermissionGuard>
);

/**
 * ConditionalButton - renders different button variants based on permissions
 */
export const ConditionalButton = ({
  account,
  requiredPermission,
  userPermission,
  requireOwnership = false,
  enabledProps = {},
  disabledProps = {},
  children,
  ...baseProps
}) => {
  const hasPermission = React.useMemo(() => {
    if (!account) return false;
    if (requireOwnership && !account.isOwner) return false;

    const currentPermission = userPermission || account.userPermission;

    if (account.isOwner) return true;
    if (!requiredPermission) return Boolean(currentPermission);

    return currentPermission && permissionIncludes(currentPermission, requiredPermission);
  }, [account, requiredPermission, userPermission, requireOwnership]);

  const buttonProps = hasPermission
    ? { ...baseProps, ...enabledProps }
    : {
        ...baseProps,
        ...disabledProps,
        disabled: true,
        title: disabledProps.title || `Requires ${requiredPermission || 'permission'}`
      };

  return React.createElement(
    baseProps.as || 'button',
    buttonProps,
    children
  );
};

export default PermissionGuard;