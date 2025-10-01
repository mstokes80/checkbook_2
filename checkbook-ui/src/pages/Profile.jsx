import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useAuth } from '../hooks/useAuth';
import { User, Mail, Save, ArrowLeft, Edit, Check, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../utils/constants';

const Profile = () => {
  const { user, updateProfile, loading, error } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');
  const [saveSuccess, setSaveSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty }
  } = useForm({
    defaultValues: {
      fullName: user?.fullName || '',
      email: user?.email || '',
      username: user?.username || ''
    }
  });

  const onSubmit = async (data) => {
    try {
      setSaving(true);
      setSaveError('');
      setSaveSuccess(false);

      await updateProfile(data);

      setSaveSuccess(true);
      setIsEditing(false);

      // Clear success message after 3 seconds
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      setSaveError(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  const handleEditClick = () => {
    setIsEditing(true);
    setSaveError('');
    setSaveSuccess(false);

    // Reset form with current user data
    reset({
      fullName: user?.fullName || '',
      email: user?.email || '',
      username: user?.username || ''
    });
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setSaveError('');
    setSaveSuccess(false);

    // Reset form to original values
    reset({
      fullName: user?.fullName || '',
      email: user?.email || '',
      username: user?.username || ''
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-2"></div>
          <p className="text-muted-foreground">Loading profile...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="bg-card border-b border-border">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between py-6">
            <div className="flex items-center space-x-4">
              <Link
                to={ROUTES.DASHBOARD}
                className="inline-flex items-center text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Dashboard
              </Link>
            </div>
            <h1 className="text-2xl font-bold text-foreground">Profile Settings</h1>
            <div className="w-32"></div> {/* Spacer for centering */}
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="card p-6">
          {/* Profile Header */}
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center space-x-4">
              <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center">
                <User className="h-8 w-8 text-primary" />
              </div>
              <div>
                <h2 className="text-xl font-semibold text-card-foreground">
                  {user?.fullName || 'User'}
                </h2>
                <p className="text-sm text-muted-foreground">
                  Member since {new Date().getFullYear()}
                </p>
              </div>
            </div>
            {!isEditing && (
              <button
                onClick={handleEditClick}
                className="inline-flex items-center px-3 py-2 border border-border rounded-md shadow-sm text-sm font-medium text-foreground bg-background hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring"
              >
                <Edit className="h-4 w-4 mr-2" />
                Edit Profile
              </button>
            )}
          </div>

          {/* Success Message */}
          {saveSuccess && (
            <div className="mb-4 p-3 bg-green-100 border border-green-400 text-green-700 rounded">
              <div className="flex items-center">
                <Check className="h-4 w-4 mr-2" />
                Profile updated successfully!
              </div>
            </div>
          )}

          {/* Error Messages */}
          {(error || saveError) && (
            <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
              {error || saveError}
            </div>
          )}

          {/* Profile Form */}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Full Name */}
            <div>
              <label htmlFor="fullName" className="block text-sm font-medium text-foreground mb-1">
                Full Name
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                <input
                  type="text"
                  id="fullName"
                  {...register('fullName', {
                    required: 'Full name is required',
                    minLength: { value: 2, message: 'Full name must be at least 2 characters' },
                    maxLength: { value: 100, message: 'Full name must be less than 100 characters' }
                  })}
                  disabled={!isEditing}
                  className="pl-10 w-full rounded-md border-border shadow-sm focus:border-ring focus:ring-ring disabled:bg-muted disabled:text-muted-foreground"
                  placeholder="Enter your full name"
                />
              </div>
              {errors.fullName && (
                <p className="mt-1 text-sm text-red-600">{errors.fullName.message}</p>
              )}
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-foreground mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                <input
                  type="email"
                  id="email"
                  {...register('email', {
                    required: 'Email is required',
                    pattern: {
                      value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                      message: 'Invalid email address'
                    }
                  })}
                  disabled={!isEditing}
                  className="pl-10 w-full rounded-md border-border shadow-sm focus:border-ring focus:ring-ring disabled:bg-muted disabled:text-muted-foreground"
                  placeholder="Enter your email address"
                />
              </div>
              {errors.email && (
                <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
              )}
            </div>

            {/* Username */}
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-foreground mb-1">
                Username
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                <input
                  type="text"
                  id="username"
                  {...register('username', {
                    required: 'Username is required',
                    minLength: { value: 3, message: 'Username must be at least 3 characters' },
                    maxLength: { value: 50, message: 'Username must be less than 50 characters' },
                    pattern: {
                      value: /^[a-zA-Z0-9_-]+$/,
                      message: 'Username can only contain letters, numbers, underscores, and hyphens'
                    }
                  })}
                  disabled={!isEditing}
                  className="pl-10 w-full rounded-md border-border shadow-sm focus:border-ring focus:ring-ring disabled:bg-muted disabled:text-muted-foreground"
                  placeholder="Enter your username"
                />
              </div>
              {errors.username && (
                <p className="mt-1 text-sm text-red-600">{errors.username.message}</p>
              )}
            </div>

            {/* Form Actions */}
            {isEditing && (
              <div className="flex justify-end space-x-3 pt-4 border-t border-border">
                <button
                  type="button"
                  onClick={handleCancelEdit}
                  className="px-4 py-2 border border-border rounded-md shadow-sm text-sm font-medium text-foreground bg-background hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring"
                >
                  <X className="h-4 w-4 mr-2 inline" />
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saving || !isDirty}
                  className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-ring disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {saving ? (
                    <>
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                      Saving...
                    </>
                  ) : (
                    <>
                      <Save className="h-4 w-4 mr-2" />
                      Save Changes
                    </>
                  )}
                </button>
              </div>
            )}
          </form>

          {/* Account Information */}
          <div className="mt-8 pt-6 border-t border-border">
            <h3 className="text-lg font-medium text-card-foreground mb-4">Account Information</h3>
            <div className="grid grid-cols-1 gap-4 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Account ID:</span>
                <span className="text-foreground font-mono">{user?.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Email Verified:</span>
                <span className={`${user?.emailVerified ? 'text-green-600' : 'text-yellow-600'} font-medium`}>
                  {user?.emailVerified ? 'Verified' : 'Not Verified'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default Profile;