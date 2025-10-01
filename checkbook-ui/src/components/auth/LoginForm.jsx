import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { ROUTES, VALIDATION } from '../../utils/constants';
import { cn } from '../../utils/cn';

const LoginForm = () => {
  const [formData, setFormData] = useState({
    usernameOrEmail: '',
    password: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState({});

  const { login, error, clearError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = location.state?.from?.pathname || ROUTES.DASHBOARD;

  const validateForm = () => {
    const errors = {};

    if (!formData.usernameOrEmail.trim()) {
      errors.usernameOrEmail = 'Username or email is required';
    } else if (formData.usernameOrEmail.length < VALIDATION.USERNAME.MIN_LENGTH) {
      errors.usernameOrEmail = `Username or email must be at least ${VALIDATION.USERNAME.MIN_LENGTH} characters`;
    }

    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < VALIDATION.PASSWORD.MIN_LENGTH) {
      errors.password = `Password must be at least ${VALIDATION.PASSWORD.MIN_LENGTH} characters`;
    }

    return errors;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    if (formErrors[name]) {
      setFormErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }

    if (error) {
      clearError();
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const errors = validateForm();
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setIsSubmitting(true);
    setFormErrors({});

    try {
      await login(formData);
      navigate(from, { replace: true });
    } catch (err) {
      console.error('Login failed:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-bold text-foreground">
            Sign in to your account
          </h2>
          <p className="mt-2 text-center text-sm text-muted-foreground">
            Or{' '}
            <Link
              to={ROUTES.REGISTER}
              className="font-medium text-primary hover:text-primary/80 transition-colors"
            >
              create a new account
            </Link>
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="rounded-md bg-destructive/10 border border-destructive/20 p-4">
              <div className="text-sm text-destructive">{error}</div>
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label htmlFor="usernameOrEmail" className="label">
                Username or Email
              </label>
              <input
                id="usernameOrEmail"
                name="usernameOrEmail"
                type="text"
                autoComplete="username"
                required
                className={cn(
                  "input mt-1",
                  formErrors.usernameOrEmail && "border-destructive focus-visible:ring-destructive"
                )}
                placeholder="Enter your username or email"
                value={formData.usernameOrEmail}
                onChange={handleChange}
                disabled={isSubmitting}
              />
              {formErrors.usernameOrEmail && (
                <p className="mt-1 text-sm text-destructive">{formErrors.usernameOrEmail}</p>
              )}
            </div>

            <div>
              <label htmlFor="password" className="label">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                autoComplete="current-password"
                required
                className={cn(
                  "input mt-1",
                  formErrors.password && "border-destructive focus-visible:ring-destructive"
                )}
                placeholder="Enter your password"
                value={formData.password}
                onChange={handleChange}
                disabled={isSubmitting}
              />
              {formErrors.password && (
                <p className="mt-1 text-sm text-destructive">{formErrors.password}</p>
              )}
            </div>
          </div>

          <div className="flex items-center justify-between">
            <Link
              to={ROUTES.FORGOT_PASSWORD}
              className="text-sm text-primary hover:text-primary/80 transition-colors"
            >
              Forgot your password?
            </Link>
          </div>

          <div>
            <button
              type="submit"
              disabled={isSubmitting}
              className={cn(
                "btn btn-primary w-full",
                isSubmitting && "opacity-50 cursor-not-allowed"
              )}
            >
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default LoginForm;