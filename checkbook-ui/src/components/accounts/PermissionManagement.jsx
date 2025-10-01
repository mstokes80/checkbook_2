import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { X, Plus, Users, Eye, Edit, CreditCard, Trash2, Mail, User, Shield, ShieldCheck } from 'lucide-react';
import { accountService } from '../../services/accountService';
import PermissionBadge, { getPermissionDescription } from './PermissionBadge';

const PermissionManagement = ({ accountId, onClose, onUpdate }) => {
  const [permissions, setPermissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors }
  } = useForm({
    defaultValues: {
      usernameOrEmail: '',
      permissionType: 'VIEW_ONLY'
    }
  });

  useEffect(() => {
    loadPermissions();
  }, [accountId]);

  const loadPermissions = async () => {
    try {
      setLoading(true);
      setError('');
      const response = await accountService.getAccountPermissions(accountId);
      if (response.success) {
        setPermissions(response.data || []);
      } else {
        setError(response.message || 'Failed to load permissions');
      }
    } catch (err) {
      setError('Failed to load permissions');
      console.error('Error loading permissions:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddPermission = async (data) => {
    try {
      setSubmitting(true);
      setError('');

      const response = await accountService.grantPermission(accountId, data);
      if (response.success) {
        await loadPermissions();
        setShowAddForm(false);
        reset();
        if (onUpdate) onUpdate();
      } else {
        setError(response.message || 'Failed to grant permission');
      }
    } catch (err) {
      setError('Failed to grant permission');
      console.error('Error granting permission:', err);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRevokePermission = async (userId) => {
    if (!window.confirm('Are you sure you want to revoke this permission?')) return;

    try {
      setError('');
      const response = await accountService.revokePermission(accountId, userId);
      if (response.success) {
        await loadPermissions();
        if (onUpdate) onUpdate();
      } else {
        setError(response.message || 'Failed to revoke permission');
      }
    } catch (err) {
      setError('Failed to revoke permission');
      console.error('Error revoking permission:', err);
    }
  };


  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="card max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-foreground">Manage Permissions</h2>
              <p className="text-muted-foreground">Control who has access to this account</p>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-muted rounded-lg transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-6 p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
              <p className="text-destructive text-sm">{error}</p>
            </div>
          )}

          {/* Add Permission Button */}
          <div className="mb-6">
            <button
              onClick={() => setShowAddForm(!showAddForm)}
              className="btn btn-primary"
            >
              <Plus className="h-4 w-4 mr-2" />
              {showAddForm ? 'Cancel' : 'Grant Permission'}
            </button>
          </div>

          {/* Add Permission Form */}
          {showAddForm && (
            <div className="card p-6 mb-6 bg-muted/30">
              <h3 className="text-lg font-semibold text-foreground mb-4">Grant New Permission</h3>
              <form onSubmit={handleSubmit(handleAddPermission)} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="usernameOrEmail" className="label mb-2 block">
                      Username or Email *
                    </label>
                    <input
                      {...register('usernameOrEmail', {
                        required: 'Username or email is required',
                        pattern: {
                          value: /^[^\s@]+@[^\s@]+\.[^\s@]+$|^[a-zA-Z0-9_]+$/,
                          message: 'Enter a valid email or username'
                        }
                      })}
                      type="text"
                      id="usernameOrEmail"
                      placeholder="user@example.com or username"
                      className="input w-full"
                    />
                    {errors.usernameOrEmail && (
                      <p className="text-destructive text-sm mt-1">{errors.usernameOrEmail.message}</p>
                    )}
                  </div>

                  <div>
                    <label htmlFor="permissionType" className="label mb-2 block">
                      Permission Level *
                    </label>
                    <select
                      {...register('permissionType')}
                      id="permissionType"
                      className="input w-full"
                    >
                      <option value="VIEW_ONLY">View Only</option>
                      <option value="TRANSACTION_ONLY">Transaction Only</option>
                      <option value="FULL_ACCESS">Full Access</option>
                    </select>
                  </div>
                </div>

                <div className="bg-muted/50 p-4 rounded-lg">
                  <h4 className="font-medium text-foreground mb-3">Permission Hierarchy:</h4>
                  <div className="space-y-3 text-sm">
                    <div className="flex items-center space-x-3">
                      <Eye className="h-4 w-4 text-blue-600" />
                      <span className="font-medium">View Only:</span>
                      <span className="text-muted-foreground">{getPermissionDescription('VIEW_ONLY')}</span>
                    </div>
                    <div className="flex items-center space-x-3">
                      <CreditCard className="h-4 w-4 text-orange-600" />
                      <span className="font-medium">Transaction Only:</span>
                      <span className="text-muted-foreground">{getPermissionDescription('TRANSACTION_ONLY')}</span>
                    </div>
                    <div className="flex items-center space-x-3">
                      <Edit className="h-4 w-4 text-green-600" />
                      <span className="font-medium">Full Access:</span>
                      <span className="text-muted-foreground">{getPermissionDescription('FULL_ACCESS')}</span>
                    </div>
                  </div>
                  <div className="mt-3 pt-3 border-t border-border/50">
                    <p className="text-xs text-muted-foreground">
                      • Higher permission levels include all capabilities of lower levels
                    </p>
                  </div>
                </div>

                <div className="flex justify-end space-x-3">
                  <button
                    type="button"
                    onClick={() => {
                      setShowAddForm(false);
                      reset();
                    }}
                    className="btn btn-outline"
                    disabled={submitting}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={submitting}
                  >
                    {submitting ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                        Granting...
                      </>
                    ) : (
                      <>
                        <ShieldCheck className="h-4 w-4 mr-2" />
                        Grant Permission
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* Permissions List */}
          <div>
            <h3 className="text-lg font-semibold text-foreground mb-4">
              Current Permissions ({permissions.length})
            </h3>

            {loading ? (
              <div className="text-center py-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
                <p className="mt-2 text-muted-foreground">Loading permissions...</p>
              </div>
            ) : permissions.length === 0 ? (
              <div className="text-center py-12">
                <Users className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                <h4 className="text-lg font-semibold text-foreground mb-2">No permissions granted</h4>
                <p className="text-muted-foreground mb-4">
                  This account is private. Grant permissions to share it with other users.
                </p>
                <button
                  onClick={() => setShowAddForm(true)}
                  className="btn btn-primary"
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Grant First Permission
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {permissions.map((permission) => (
                  <div
                    key={permission.id}
                    className="card p-4 flex items-center justify-between hover:shadow-md transition-shadow"
                  >
                    <div className="flex items-center space-x-4">
                      <div className="flex-shrink-0">
                        <div className="p-2 bg-muted rounded-lg">
                          <User className="h-5 w-5 text-muted-foreground" />
                        </div>
                      </div>
                      <div>
                        <div className="flex items-center space-x-3">
                          <h4 className="font-semibold text-foreground">
                            {permission.fullName}
                          </h4>
                          <PermissionBadge permissionType={permission.permissionType} size="small" />
                        </div>
                        <div className="flex items-center space-x-4 mt-1 text-sm text-muted-foreground">
                          <div className="flex items-center space-x-1">
                            <Mail className="h-3 w-3" />
                            <span>{permission.email}</span>
                          </div>
                          <div className="flex items-center space-x-1">
                            <User className="h-3 w-3" />
                            <span>@{permission.username}</span>
                          </div>
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">
                          Granted {new Date(permission.createdAt).toLocaleDateString()}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => handleRevokePermission(permission.userId)}
                        className="btn btn-outline text-destructive hover:bg-destructive hover:text-destructive-foreground"
                        title="Revoke Permission"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="mt-8 pt-6 border-t border-border">
            <div className="flex justify-between items-center">
              <div className="text-sm text-muted-foreground">
                <p>• You maintain full ownership and control of this account</p>
                <p>• Permissions can be revoked at any time</p>
              </div>
              <button
                onClick={onClose}
                className="btn btn-outline"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PermissionManagement;