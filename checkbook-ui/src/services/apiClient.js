import axios from 'axios';
import { API_BASE_URL, API_ENDPOINTS } from '../utils/constants';
import tokenManager from '../utils/tokenManager';

// Shared axios instance for every service, so token handling lives in one place.
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

// Concurrent 401s share one refresh request instead of each spending the refresh token.
let refreshPromise = null;

const refreshAccessToken = async () => {
  const refreshToken = tokenManager.getRefreshToken();
  const response = await axios.post(`${API_BASE_URL}${API_ENDPOINTS.AUTH.REFRESH}`, {
    refreshToken
  });

  // The backend wraps the JwtResponse in ApiResponse: { success, message, data }
  if (!response.data.success || !response.data.data?.token) {
    throw new Error('Token refresh failed');
  }

  const { token, refreshToken: newRefreshToken, id, username, email, fullName, emailVerified } = response.data.data;
  const user = { id, username, email, fullName, emailVerified };
  tokenManager.setAuthData(token, newRefreshToken, user);
  return token;
};

// Response interceptor for token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      if (tokenManager.getRefreshToken()) {
        try {
          refreshPromise = refreshPromise || refreshAccessToken().finally(() => {
            refreshPromise = null;
          });
          const token = await refreshPromise;

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

export default api;
