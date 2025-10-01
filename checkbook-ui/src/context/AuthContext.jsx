import React, { createContext, useContext, useReducer, useEffect } from 'react';
import tokenManager from '../utils/tokenManager';
import authService from '../services/authService';

const AuthContext = createContext();

const authReducer = (state, action) => {
  switch (action.type) {
    case 'LOGIN_START':
      return {
        ...state,
        loading: true,
        error: null,
      };
    case 'LOGIN_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        isAuthenticated: true,
        loading: false,
        error: null,
      };
    case 'LOGIN_FAILURE':
      return {
        ...state,
        user: null,
        isAuthenticated: false,
        loading: false,
        error: action.payload.error,
      };
    case 'LOGOUT':
      return {
        ...state,
        user: null,
        isAuthenticated: false,
        loading: false,
        error: null,
      };
    case 'UPDATE_USER':
      return {
        ...state,
        user: action.payload.user,
      };
    case 'SET_LOADING':
      return {
        ...state,
        loading: action.payload.loading,
      };
    case 'SET_ERROR':
      return {
        ...state,
        error: action.payload.error,
        loading: false,
      };
    case 'CLEAR_ERROR':
      return {
        ...state,
        error: null,
      };
    default:
      return state;
  }
};

const initialState = {
  user: null,
  isAuthenticated: false,
  loading: true,
  error: null,
};

export const AuthProvider = ({ children }) => {
  const [state, dispatch] = useReducer(authReducer, initialState);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = tokenManager.getToken();
        const user = tokenManager.getUser();

        if (token && user && !tokenManager.isTokenExpired(token)) {
          dispatch({
            type: 'LOGIN_SUCCESS',
            payload: { user },
          });
        } else if (token && tokenManager.isTokenExpired(token)) {
          const refreshToken = tokenManager.getRefreshToken();
          if (refreshToken) {
            try {
              const response = await authService.refreshToken();
              if (response.success && response.data) {
                const jwtResponse = response.data;
                const { id, username, email, fullName, emailVerified } = jwtResponse;
                const user = { id, username, email, fullName, emailVerified };
                dispatch({
                  type: 'LOGIN_SUCCESS',
                  payload: { user },
                });
              }
            } catch (error) {
              tokenManager.clearAuthData();
              dispatch({ type: 'LOGOUT' });
            }
          } else {
            tokenManager.clearAuthData();
            dispatch({ type: 'LOGOUT' });
          }
        } else {
          dispatch({ type: 'LOGOUT' });
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        tokenManager.clearAuthData();
        dispatch({ type: 'LOGOUT' });
      } finally {
        dispatch({ type: 'SET_LOADING', payload: { loading: false } });
      }
    };

    initializeAuth();
  }, []);

  const login = async (credentials) => {
    try {
      dispatch({ type: 'LOGIN_START' });
      const response = await authService.login(credentials);
      if (response.success && response.data) {
        const jwtResponse = response.data;
        const { id, username, email, fullName, emailVerified } = jwtResponse;
        const user = { id, username, email, fullName, emailVerified };
        dispatch({
          type: 'LOGIN_SUCCESS',
          payload: { user },
        });
      }
      return response;
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Login failed';
      dispatch({
        type: 'LOGIN_FAILURE',
        payload: { error: errorMessage },
      });
      throw error;
    }
  };

  const register = async (userData) => {
    try {
      dispatch({ type: 'LOGIN_START' });
      const response = await authService.register(userData);
      if (response.success && response.data) {
        const jwtResponse = response.data;
        const { id, username, email, fullName, emailVerified } = jwtResponse;
        const user = { id, username, email, fullName, emailVerified };
        dispatch({
          type: 'LOGIN_SUCCESS',
          payload: { user },
        });
      }
      return response;
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Registration failed';
      dispatch({
        type: 'LOGIN_FAILURE',
        payload: { error: errorMessage },
      });
      throw error;
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.warn('Logout request failed:', error);
    } finally {
      dispatch({ type: 'LOGOUT' });
    }
  };

  const updateProfile = async (profileData) => {
    try {
      const updatedUser = await authService.updateProfile(profileData);
      dispatch({
        type: 'UPDATE_USER',
        payload: { user: updatedUser },
      });
      return updatedUser;
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Profile update failed';
      dispatch({
        type: 'SET_ERROR',
        payload: { error: errorMessage },
      });
      throw error;
    }
  };

  const forgotPassword = async (email) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { loading: true } });
      const response = await authService.forgotPassword(email);
      dispatch({ type: 'SET_LOADING', payload: { loading: false } });
      return response;
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Password reset request failed';
      dispatch({
        type: 'SET_ERROR',
        payload: { error: errorMessage },
      });
      throw error;
    }
  };

  const resetPassword = async (token, newPassword) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { loading: true } });
      const response = await authService.resetPassword(token, newPassword);
      dispatch({ type: 'SET_LOADING', payload: { loading: false } });
      return response;
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Password reset failed';
      dispatch({
        type: 'SET_ERROR',
        payload: { error: errorMessage },
      });
      throw error;
    }
  };

  const clearError = () => {
    dispatch({ type: 'CLEAR_ERROR' });
  };

  const value = {
    ...state,
    login,
    register,
    logout,
    updateProfile,
    forgotPassword,
    resetPassword,
    clearError,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;