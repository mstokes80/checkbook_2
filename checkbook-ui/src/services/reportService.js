import axios from 'axios';
import { API_BASE_URL } from '../utils/constants';
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

// Response interceptor for token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = tokenManager.getRefreshToken();
      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken
          });

          if (response.data.success && response.data.data.token) {
            const jwtResponse = response.data.data;
            const { token, refreshToken: newRefreshToken, id, username, email, fullName, emailVerified } = jwtResponse;
            const user = { id, username, email, fullName, emailVerified };
            tokenManager.setAuthData(token, newRefreshToken, user);

            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          }
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

export const reportService = {
  /**
   * Generate a spending report
   * @param {string} startDate - Start date in YYYY-MM-DD format
   * @param {string} endDate - End date in YYYY-MM-DD format
   * @param {Array<number>} accountIds - Optional array of account IDs
   * @returns {Promise} Report data
   */
  async generateReport(startDate, endDate, accountIds = null) {
    const params = {
      startDate,
      endDate
    };

    if (accountIds && accountIds.length > 0) {
      params.accountIds = accountIds.join(',');
    }

    const response = await api.get('/reports/spending', { params });
    return response.data;
  },

  /**
   * Export report as PDF
   * @param {string} startDate - Start date in YYYY-MM-DD format
   * @param {string} endDate - End date in YYYY-MM-DD format
   * @param {Array<number>} accountIds - Optional array of account IDs
   */
  async exportPdf(startDate, endDate, accountIds = null) {
    const params = {
      startDate,
      endDate
    };

    if (accountIds && accountIds.length > 0) {
      params.accountIds = accountIds.join(',');
    }

    const response = await api.get('/reports/spending/export/pdf', {
      params,
      responseType: 'blob'
    });

    // Create blob and download
    const blob = new Blob([response.data], { type: 'application/pdf' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `spending-report-${startDate}-${endDate}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },

  /**
   * Export report as CSV
   * @param {string} startDate - Start date in YYYY-MM-DD format
   * @param {string} endDate - End date in YYYY-MM-DD format
   * @param {Array<number>} accountIds - Optional array of account IDs
   */
  async exportCsv(startDate, endDate, accountIds = null) {
    const params = {
      startDate,
      endDate
    };

    if (accountIds && accountIds.length > 0) {
      params.accountIds = accountIds.join(',');
    }

    const response = await api.get('/reports/spending/export/csv', {
      params,
      responseType: 'blob'
    });

    // Create blob and download
    const blob = new Blob([response.data], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `spending-report-${startDate}-${endDate}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
};

export default reportService;