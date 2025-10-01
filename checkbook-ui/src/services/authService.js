import axios from 'axios';
import { API_BASE_URL, API_ENDPOINTS } from '../utils/constants';
import tokenManager from '../utils/tokenManager';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

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

          if (response.data.success && response.data.data.token) {
            const jwtResponse = response.data.data;
            const { token, refreshToken: newRefreshToken, id, username, email, fullName, emailVerified } = jwtResponse;
            const user = { id, username, email, fullName, emailVerified };
            tokenManager.setAuthData(token, newRefreshToken, user);
          }

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

export const authService = {
  async register(userData) {
    const response = await api.post(API_ENDPOINTS.AUTH.REGISTER, userData);
    if (response.data.success && response.data.data.token) {
      const jwtResponse = response.data.data;
      const { token, refreshToken, id, username, email, fullName, emailVerified } = jwtResponse;
      const user = { id, username, email, fullName, emailVerified };
      tokenManager.setAuthData(token, refreshToken, user);
    }
    return response.data;
  },

  async login(credentials) {
    const response = await api.post(API_ENDPOINTS.AUTH.LOGIN, credentials);
    if (response.data.success && response.data.data.token) {
      const jwtResponse = response.data.data;
      const { token, refreshToken, id, username, email, fullName, emailVerified } = jwtResponse;
      const user = { id, username, email, fullName, emailVerified };
      tokenManager.setAuthData(token, refreshToken, user);
    }
    return response.data;
  },

  async logout() {
    try {
      await api.post(API_ENDPOINTS.AUTH.LOGOUT);
    } catch (error) {
      console.warn('Logout request failed:', error);
    } finally {
      tokenManager.clearAuthData();
    }
  },

  async getCurrentUser() {
    const response = await api.get(API_ENDPOINTS.AUTH.ME);
    return response.data;
  },

  async updateProfile(profileData) {
    const response = await api.put(API_ENDPOINTS.AUTH.PROFILE, profileData);
    const updatedUser = response.data;
    tokenManager.setUser(updatedUser);
    return updatedUser;
  },

  async forgotPassword(email) {
    const response = await api.post(API_ENDPOINTS.AUTH.FORGOT_PASSWORD, { email });
    return response.data;
  },

  async validateResetToken(token) {
    const response = await api.post(API_ENDPOINTS.AUTH.VALIDATE_RESET_TOKEN, { token });
    return response.data;
  },

  async resetPassword(token, newPassword) {
    const response = await api.post(API_ENDPOINTS.AUTH.RESET_PASSWORD, {
      token,
      newPassword
    });
    return response.data;
  },

  async refreshToken() {
    const refreshToken = tokenManager.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await api.post(API_ENDPOINTS.AUTH.REFRESH, { refreshToken });
    if (response.data.success && response.data.data.token) {
      const jwtResponse = response.data.data;
      const { token, refreshToken: newRefreshToken, id, username, email, fullName, emailVerified } = jwtResponse;
      const user = { id, username, email, fullName, emailVerified };
      tokenManager.setAuthData(token, newRefreshToken, user);
    }
    return response.data;
  }
};

export default authService;