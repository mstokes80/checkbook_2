// API Configuration
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// Authentication Constants
export const TOKEN_KEY = 'checkbook_token';
export const REFRESH_TOKEN_KEY = 'checkbook_refresh_token';
export const USER_KEY = 'checkbook_user';

// API Endpoints
export const API_ENDPOINTS = {
  AUTH: {
    REGISTER: '/auth/register',
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh',
    ME: '/auth/me',
    PROFILE: '/auth/profile',
    FORGOT_PASSWORD: '/auth/forgot-password',
    RESET_PASSWORD: '/auth/reset-password',
    VALIDATE_RESET_TOKEN: '/auth/validate-reset-token',
  },
  ACCOUNTS: {
    BASE: '/accounts',
    DASHBOARD: '/accounts/dashboard',
    BY_ID: (id) => `/accounts/${id}`,
    BALANCE: (id) => `/accounts/${id}/balance`,
    PERMISSIONS: (id) => `/accounts/${id}/permissions`,
    PERMISSION_BY_USER: (accountId, userId) => `/accounts/${accountId}/permissions/${userId}`,
    AUDIT_LOGS: (id) => `/accounts/${id}/audit-logs`,
  },
  TRANSACTIONS: {
    BASE: '/transactions',
    BY_ID: (id) => `/transactions/${id}`,
    RECENT: '/transactions/recent',
    STATISTICS: '/transactions/statistics',
    UNCATEGORIZED: '/transactions/uncategorized',
    BULK_CATEGORIZE: '/transactions/bulk-categorize',
  },
  TRANSACTION_CATEGORIES: {
    BASE: '/transaction-categories',
    BY_ID: (id) => `/transaction-categories/${id}`,
    SYSTEM: '/transaction-categories/system',
    USER: '/transaction-categories/user',
    SEARCH: '/transaction-categories/search',
    WITH_COUNTS: '/transaction-categories/with-counts',
    DELETABLE: '/transaction-categories/deletable',
    MOST_USED: '/transaction-categories/most-used',
    SUMMARY: '/transaction-categories/summary',
    UNCATEGORIZED: '/transaction-categories/uncategorized',
    VALIDATE_DELETION: (id) => `/transaction-categories/${id}/validate-deletion`,
  },
  REPORTS: {
    SPENDING: '/reports/spending',
    EXPORT_PDF: '/reports/spending/export/pdf',
    EXPORT_CSV: '/reports/spending/export/csv',
  }
};

// Routes
export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  DASHBOARD: '/dashboard',
  PROFILE: '/profile',
  FORGOT_PASSWORD: '/forgot-password',
  RESET_PASSWORD: '/reset-password',
  REPORTS: '/reports',
};

// Form Validation
export const VALIDATION = {
  USERNAME: {
    MIN_LENGTH: 3,
    MAX_LENGTH: 50,
  },
  PASSWORD: {
    MIN_LENGTH: 6,
    MAX_LENGTH: 100,
  },
  EMAIL: {
    MAX_LENGTH: 100,
  },
  NAME: {
    MAX_LENGTH: 50,
  }
};