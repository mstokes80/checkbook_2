import axios from 'axios';
import { API_BASE_URL, API_ENDPOINTS } from '../utils/constants';
import tokenManager from '../utils/tokenManager';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = tokenManager.getToken();
    if (token && !tokenManager.isTokenExpired(token)) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = tokenManager.getRefreshToken();
      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE_URL}${API_ENDPOINTS.AUTH.REFRESH}`, {
            refreshToken
          });

          const { token, refreshToken: newRefreshToken, user } = response.data;
          tokenManager.setAuthData(token, newRefreshToken, user);

          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        } catch (refreshError) {
          tokenManager.clearAuthData();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      } else {
        tokenManager.clearAuthData();
        window.location.href = '/login';
      }
    }

    return Promise.reject(error);
  }
);

export const accountService = {
  // Get all accessible accounts
  async getAccounts() {
    const response = await api.get(API_ENDPOINTS.ACCOUNTS.BASE);
    return response.data;
  },

  // Get account by ID
  async getAccountById(id) {
    const response = await api.get(API_ENDPOINTS.ACCOUNTS.BY_ID(id));
    return response.data;
  },

  // Create new account
  async createAccount(accountData) {
    const response = await api.post(API_ENDPOINTS.ACCOUNTS.BASE, accountData);
    return response.data;
  },

  // Update account
  async updateAccount(id, accountData) {
    const response = await api.put(API_ENDPOINTS.ACCOUNTS.BY_ID(id), accountData);
    return response.data;
  },

  // Delete account
  async deleteAccount(id) {
    const response = await api.delete(API_ENDPOINTS.ACCOUNTS.BY_ID(id));
    return response.data;
  },

  // Update account balance
  async updateAccountBalance(id, balance) {
    const response = await api.patch(API_ENDPOINTS.ACCOUNTS.BALANCE(id), { balance });
    return response.data;
  },

  // Get dashboard data
  async getDashboardData() {
    const response = await api.get(API_ENDPOINTS.ACCOUNTS.DASHBOARD);
    return response.data;
  },

  // Permission management
  async getAccountPermissions(id) {
    const response = await api.get(API_ENDPOINTS.ACCOUNTS.PERMISSIONS(id));
    return response.data;
  },

  async grantPermission(id, permissionData) {
    const response = await api.post(API_ENDPOINTS.ACCOUNTS.PERMISSIONS(id), permissionData);
    return response.data;
  },

  async revokePermission(accountId, userId) {
    const response = await api.delete(API_ENDPOINTS.ACCOUNTS.PERMISSION_BY_USER(accountId, userId));
    return response.data;
  },

  // Audit log functionality
  async getAccountAuditLogs(accountId, queryParams = {}) {
    const params = new URLSearchParams(queryParams).toString();
    const url = `${API_ENDPOINTS.ACCOUNTS.AUDIT_LOGS(accountId)}${params ? `?${params}` : ''}`;
    const response = await api.get(url);
    return response.data;
  }
};

export default accountService;