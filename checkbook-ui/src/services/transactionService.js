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
    console.log('Request interceptor - token:', token ? 'exists' : 'missing');
    console.log('Request interceptor - token expired:', token ? tokenManager.isTokenExpired(token) : 'N/A');
    if (token && !tokenManager.isTokenExpired(token)) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('Request interceptor - Authorization header set');
    } else {
      console.log('Request interceptor - No valid token, Authorization header not set');
    }
    console.log('Request interceptor - config:', config.url, config.headers);
    return config;
  },
  (error) => {
    console.error('Request interceptor error:', error);
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

export const transactionService = {
  // Create a new transaction
  async createTransaction(transactionData) {
    const response = await api.post(API_ENDPOINTS.TRANSACTIONS.BASE, transactionData);
    return response.data;
  },

  // Update an existing transaction
  async updateTransaction(id, transactionData) {
    const response = await api.put(API_ENDPOINTS.TRANSACTIONS.BY_ID(id), transactionData);
    return response.data;
  },

  // Delete a transaction
  async deleteTransaction(id) {
    const response = await api.delete(API_ENDPOINTS.TRANSACTIONS.BY_ID(id));
    return response.data;
  },

  // Get transaction by ID
  async getTransactionById(id) {
    const response = await api.get(API_ENDPOINTS.TRANSACTIONS.BY_ID(id));
    return response.data;
  },

  // Get transactions for an account with filtering and pagination
  async getAccountTransactions(queryParams = {}) {
    const params = new URLSearchParams();

    // Add required accountId parameter
    if (queryParams.accountId) {
      params.append('accountId', queryParams.accountId);
    }

    // Add optional filtering parameters
    if (queryParams.categoryId) {
      params.append('categoryId', queryParams.categoryId);
    }
    if (queryParams.startDate) {
      params.append('startDate', queryParams.startDate);
    }
    if (queryParams.endDate) {
      params.append('endDate', queryParams.endDate);
    }
    if (queryParams.search) {
      params.append('search', queryParams.search);
    }

    // Add pagination parameters
    if (queryParams.page !== undefined) {
      params.append('page', queryParams.page);
    }
    if (queryParams.size) {
      params.append('size', queryParams.size);
    }
    if (queryParams.sort) {
      params.append('sort', queryParams.sort);
    }
    if (queryParams.direction) {
      params.append('direction', queryParams.direction);
    }

    const url = `${API_ENDPOINTS.TRANSACTIONS.BASE}?${params.toString()}`;
    const response = await api.get(url);
    return response.data;
  },

  // Get recent transactions for an account
  async getRecentTransactions(accountId, limit = 10) {
    const params = new URLSearchParams({
      accountId: accountId.toString(),
      limit: limit.toString()
    });

    const url = `${API_ENDPOINTS.TRANSACTIONS.RECENT}?${params.toString()}`;
    const response = await api.get(url);
    return response.data;
  },

  // Get transaction statistics for an account
  async getTransactionStatistics(queryParams = {}) {
    const params = new URLSearchParams();

    if (queryParams.accountId) {
      params.append('accountId', queryParams.accountId);
    }
    if (queryParams.startDate) {
      params.append('startDate', queryParams.startDate);
    }
    if (queryParams.endDate) {
      params.append('endDate', queryParams.endDate);
    }

    const url = `${API_ENDPOINTS.TRANSACTIONS.STATISTICS}?${params.toString()}`;
    const response = await api.get(url);
    return response.data;
  },

  // Get uncategorized transactions for an account
  async getUncategorizedTransactions(accountId, page = 0, size = 20) {
    const params = new URLSearchParams({
      accountId: accountId.toString(),
      page: page.toString(),
      size: size.toString()
    });

    const url = `${API_ENDPOINTS.TRANSACTIONS.UNCATEGORIZED}?${params.toString()}`;
    const response = await api.get(url);
    return response.data;
  },

  // Bulk categorize transactions
  async bulkCategorizeTransactions(transactionIds, categoryId) {
    const requestData = {
      transactionIds,
      categoryId
    };

    const response = await api.post(API_ENDPOINTS.TRANSACTIONS.BULK_CATEGORIZE, requestData);
    return response.data;
  }
};

export const transactionCategoryService = {
  // Create a new transaction category
  async createCategory(categoryData) {
    const response = await api.post(API_ENDPOINTS.TRANSACTION_CATEGORIES.BASE, categoryData);
    return response.data;
  },

  // Update an existing transaction category
  async updateCategory(id, categoryData) {
    const response = await api.put(API_ENDPOINTS.TRANSACTION_CATEGORIES.BY_ID(id), categoryData);
    return response.data;
  },

  // Delete a transaction category
  async deleteCategory(id) {
    const response = await api.delete(API_ENDPOINTS.TRANSACTION_CATEGORIES.BY_ID(id));
    return response.data;
  },

  // Get transaction category by ID
  async getCategoryById(id) {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.BY_ID(id));
    return response.data;
  },

  // Get all transaction categories
  async getAllCategories() {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.BASE);
    return response.data;
  },

  // Get system default categories
  async getSystemCategories() {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.SYSTEM);
    return response.data;
  },

  // Get user-created categories
  async getUserCategories() {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.USER);
    return response.data;
  },

  // Search categories by name
  async searchCategories(searchTerm = '') {
    const params = searchTerm ? `?q=${encodeURIComponent(searchTerm)}` : '';
    const url = `${API_ENDPOINTS.TRANSACTION_CATEGORIES.SEARCH}${params}`;
    const response = await api.get(url);
    return response.data;
  },

  // Get categories with transaction counts
  async getCategoriesWithCounts() {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.WITH_COUNTS);
    return response.data;
  },

  // Get categories that can be deleted
  async getDeletableCategories() {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.DELETABLE);
    return response.data;
  },

  // Get most used categories
  async getMostUsedCategories(limit = 10) {
    const params = `?limit=${limit}`;
    const url = `${API_ENDPOINTS.TRANSACTION_CATEGORIES.MOST_USED}${params}`;
    const response = await api.get(url);
    return response.data;
  },

  // Get category summary statistics
  async getCategorySummary() {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.SUMMARY);
    return response.data;
  },

  // Get default uncategorized category
  async getUncategorizedCategory() {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.UNCATEGORIZED);
    return response.data;
  },

  // Validate if a category can be deleted
  async validateCategoryDeletion(id) {
    const response = await api.get(API_ENDPOINTS.TRANSACTION_CATEGORIES.VALIDATE_DELETION(id));
    return response.data;
  }
};

export default transactionService;