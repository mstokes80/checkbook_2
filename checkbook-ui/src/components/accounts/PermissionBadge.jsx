import React from 'react';
import { Eye, Edit, CreditCard, Shield } from 'lucide-react';

const PermissionBadge = ({
  permissionType,
  size = 'default',
  showIcon = true,
  showText = true,
  className = ''
}) => {
  const getPermissionConfig = (permissionType) => {
    switch (permissionType) {
      case 'VIEW_ONLY':
        return {
          icon: Eye,
          label: 'View Only',
          description: 'Can view account details and transaction history',
          color: 'text-blue-600 bg-blue-100 border-blue-200',
          darkColor: 'dark:text-blue-400 dark:bg-blue-950 dark:border-blue-800'
        };
      case 'TRANSACTION_ONLY':
        return {
          icon: CreditCard,
          label: 'Transaction Only',
          description: 'Can view account details and add transactions',
          color: 'text-orange-600 bg-orange-100 border-orange-200',
          darkColor: 'dark:text-orange-400 dark:bg-orange-950 dark:border-orange-800'
        };
      case 'FULL_ACCESS':
        return {
          icon: Edit,
          label: 'Full Access',
          description: 'Full access including account modification and balance updates',
          color: 'text-green-600 bg-green-100 border-green-200',
          darkColor: 'dark:text-green-400 dark:bg-green-950 dark:border-green-800'
        };
      default:
        return {
          icon: Shield,
          label: 'Unknown',
          description: 'Unknown permission level',
          color: 'text-gray-600 bg-gray-100 border-gray-200',
          darkColor: 'dark:text-gray-400 dark:bg-gray-950 dark:border-gray-800'
        };
    }
  };

  const getSizeClasses = (size) => {
    switch (size) {
      case 'small':
        return {
          container: 'px-2 py-1 text-xs',
          icon: 'h-3 w-3',
          gap: 'gap-1'
        };
      case 'large':
        return {
          container: 'px-4 py-2 text-base',
          icon: 'h-5 w-5',
          gap: 'gap-2'
        };
      default:
        return {
          container: 'px-2 py-1 text-sm',
          icon: 'h-4 w-4',
          gap: 'gap-1.5'
        };
    }
  };

  const config = getPermissionConfig(permissionType);
  const sizeClasses = getSizeClasses(size);
  const IconComponent = config.icon;

  const badgeClasses = `
    inline-flex items-center ${sizeClasses.gap} rounded-full font-medium border
    ${config.color} ${config.darkColor} ${sizeClasses.container} ${className}
  `.trim();

  return (
    <span className={badgeClasses} title={config.description}>
      {showIcon && <IconComponent className={sizeClasses.icon} />}
      {showText && <span>{config.label}</span>}
    </span>
  );
};

// Helper function to get permission hierarchy level (for sorting/comparison)
export const getPermissionLevel = (permissionType) => {
  switch (permissionType) {
    case 'VIEW_ONLY':
      return 1;
    case 'TRANSACTION_ONLY':
      return 2;
    case 'FULL_ACCESS':
      return 3;
    default:
      return 0;
  }
};

// Helper function to check if permission includes another permission
export const permissionIncludes = (userPermission, requiredPermission) => {
  return getPermissionLevel(userPermission) >= getPermissionLevel(requiredPermission);
};

// Helper function to get all available permission types
export const getAvailablePermissions = () => [
  'VIEW_ONLY',
  'TRANSACTION_ONLY',
  'FULL_ACCESS'
];

// Helper function to get permission description
export const getPermissionDescription = (permissionType) => {
  switch (permissionType) {
    case 'VIEW_ONLY':
      return 'Can view account details and transaction history';
    case 'TRANSACTION_ONLY':
      return 'Can view account details and add transactions';
    case 'FULL_ACCESS':
      return 'Full access including account modification and balance updates';
    default:
      return 'Unknown permission level';
  }
};

// Helper function to format permission type for display
export const formatPermissionType = (permissionType) => {
  if (!permissionType) return 'Unknown';
  return permissionType.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase());
};

export default PermissionBadge;