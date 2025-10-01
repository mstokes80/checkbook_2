import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { ROUTES, VALIDATION } from '../../utils/constants';
import { cn } from '../../utils/cn';

const RegisterForm = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    confirmPassword: '',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formErrors, setFormErrors] = useState({});

  const { register, error, clearError } = useAuth();
  const navigate = useNavigate();

  const validateForm = () => {
    const errors = {};

    if (!formData.username.trim()) {
      errors.username = 'Username is required';
    } else if (formData.username.length < VALIDATION.USERNAME.MIN_LENGTH) {
      errors.username = `Username must be at least ${VALIDATION.USERNAME.MIN_LENGTH} characters`;
    } else if (formData.username.length > VALIDATION.USERNAME.MAX_LENGTH) {
      errors.username = `Username must be no more than ${VALIDATION.USERNAME.MAX_LENGTH} characters`;
    }

    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Please enter a valid email address';
    } else if (formData.email.length > VALIDATION.EMAIL.MAX_LENGTH) {
      errors.email = `Email must be no more than ${VALIDATION.EMAIL.MAX_LENGTH} characters`;
    }

    if (!formData.firstName.trim()) {
      errors.firstName = 'First name is required';
    } else if (formData.firstName.length > VALIDATION.NAME.MAX_LENGTH) {
      errors.firstName = `First name must be no more than ${VALIDATION.NAME.MAX_LENGTH} characters`;
    }

    if (!formData.lastName.trim()) {
      errors.lastName = 'Last name is required';
    } else if (formData.lastName.length > VALIDATION.NAME.MAX_LENGTH) {
      errors.lastName = `Last name must be no more than ${VALIDATION.NAME.MAX_LENGTH} characters`;
    }

    if (!formData.password) {
      errors.password = 'Password is required';
    } else if (formData.password.length < VALIDATION.PASSWORD.MIN_LENGTH) {
      errors.password = `Password must be at least ${VALIDATION.PASSWORD.MIN_LENGTH} characters`;
    } else if (formData.password.length > VALIDATION.PASSWORD.MAX_LENGTH) {
      errors.password = `Password must be no more than ${VALIDATION.PASSWORD.MAX_LENGTH} characters`;
    }

    if (!formData.confirmPassword) {
      errors.confirmPassword = 'Please confirm your password';
    } else if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
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
      const { confirmPassword, ...userData } = formData;
      await register(userData);
      navigate(ROUTES.DASHBOARD);
    } catch (err) {
      console.error('Registration failed:', err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-bold text-foreground">
            Create your account
          </h2>
          <p className="mt-2 text-center text-sm text-muted-foreground">
            Or{' '}
            <Link
              to={ROUTES.LOGIN}
              className="font-medium text-primary hover:text-primary/80 transition-colors"
            >
              sign in to your existing account
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
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="firstName" className="label">
                  First Name
                </label>
                <input
                  id="firstName"
                  name="firstName"
                  type="text"
                  autoComplete="given-name"
                  required
                  className={cn(
                    "input mt-1",
                    formErrors.firstName && "border-destructive focus-visible:ring-destructive"
                  )}
                  placeholder="First name"
                  value={formData.firstName}
                  onChange={handleChange}
                  disabled={isSubmitting}
                />
                {formErrors.firstName && (
                  <p className="mt-1 text-sm text-destructive">{formErrors.firstName}</p>
                )}
              </div>

              <div>
                <label htmlFor="lastName" className="label">
                  Last Name
                </label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  autoComplete="family-name"
                  required
                  className={cn(
                    "input mt-1",
                    formErrors.lastName && "border-destructive focus-visible:ring-destructive"
                  )}
                  placeholder="Last name"
                  value={formData.lastName}
                  onChange={handleChange}
                  disabled={isSubmitting}
                />
                {formErrors.lastName && (
                  <p className="mt-1 text-sm text-destructive">{formErrors.lastName}</p>
                )}
              </div>
            </div>

            <div>
              <label htmlFor="username" className="label">
                Username
              </label>
              <input
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                required
                className={cn(
                  "input mt-1",
                  formErrors.username && "border-destructive focus-visible:ring-destructive"
                )}
                placeholder="Choose a username"
                value={formData.username}
                onChange={handleChange}
                disabled={isSubmitting}
              />
              {formErrors.username && (
                <p className="mt-1 text-sm text-destructive">{formErrors.username}</p>
              )}
            </div>

            <div>
              <label htmlFor="email" className="label">
                Email Address
              </label>
              <input
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                required
                className={cn(
                  "input mt-1",
                  formErrors.email && "border-destructive focus-visible:ring-destructive"
                )}
                placeholder="Enter your email"
                value={formData.email}
                onChange={handleChange}
                disabled={isSubmitting}
              />
              {formErrors.email && (
                <p className="mt-1 text-sm text-destructive">{formErrors.email}</p>
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
                autoComplete="new-password"
                required
                className={cn(
                  "input mt-1",
                  formErrors.password && "border-destructive focus-visible:ring-destructive"
                )}
                placeholder="Create a password"
                value={formData.password}
                onChange={handleChange}
                disabled={isSubmitting}
              />
              {formErrors.password && (
                <p className="mt-1 text-sm text-destructive">{formErrors.password}</p>
              )}
            </div>

            <div>
              <label htmlFor="confirmPassword" className="label">
                Confirm Password
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                required
                className={cn(
                  "input mt-1",
                  formErrors.confirmPassword && "border-destructive focus-visible:ring-destructive"
                )}
                placeholder="Confirm your password"
                value={formData.confirmPassword}
                onChange={handleChange}
                disabled={isSubmitting}
              />
              {formErrors.confirmPassword && (
                <p className="mt-1 text-sm text-destructive">{formErrors.confirmPassword}</p>
              )}
            </div>
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
              {isSubmitting ? 'Creating account...' : 'Create account'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RegisterForm;