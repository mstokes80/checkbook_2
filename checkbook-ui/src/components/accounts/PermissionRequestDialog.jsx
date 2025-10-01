import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, ArrowUp, Send, AlertCircle, Info } from 'lucide-react';
import PermissionBadge, {
  getPermissionLevel,
  getPermissionDescription,
  getAvailablePermissions
} from './PermissionBadge';

const PermissionRequestDialog = ({
  account,
  currentPermission,
  onClose,
  onRequestSubmitted,
  permissionRequestService
}) => {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors }
  } = useForm({
    defaultValues: {
      requestedPermission: getNextAvailablePermission(currentPermission),
      reason: ''
    }
  });

  // Get available permission options for escalation
  function getAvailablePermissionOptions(currentPermission) {
    const currentLevel = getPermissionLevel(currentPermission);
    const allPermissions = getAvailablePermissions();

    return allPermissions.filter(permission =>
      getPermissionLevel(permission) > currentLevel
    );
  }

  function getNextAvailablePermission(currentPermission) {
    const available = getAvailablePermissionOptions(currentPermission);
    return available.length > 0 ? available[0] : 'FULL_ACCESS';
  }

  const requestedPermission = watch('requestedPermission');
  const availableOptions = getAvailablePermissionOptions(currentPermission);

  const handleRequestSubmit = async (data) => {
    try {
      setSubmitting(true);
      setError('');

      const response = await permissionRequestService.createPermissionRequest({
        accountId: account.id,
        requestedPermission: data.requestedPermission,
        reason: data.reason
      });

      if (response.success) {
        if (onRequestSubmitted) {
          onRequestSubmitted(response.data);
        }
        onClose();
      } else {
        setError(response.message || 'Failed to submit permission request');
      }
    } catch (err) {
      setError('Failed to submit permission request');
      console.error('Error submitting permission request:', err);
    } finally {
      setSubmitting(false);
    }
  };

  // Don't show dialog if user can't escalate (already has highest permission)
  if (!currentPermission || availableOptions.length === 0) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-foreground">Request Permission Upgrade</h2>
              <p className="text-muted-foreground">
                Request higher access level for "{account.name}"
              </p>
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
            <div className="mb-6 p-4 bg-destructive/10 border border-destructive/20 rounded-lg flex items-center space-x-2">
              <AlertCircle className="h-4 w-4 text-destructive" />
              <p className="text-destructive text-sm">{error}</p>
            </div>
          )}

          {/* Current Permission Status */}
          <div className="mb-6 p-4 bg-muted/30 rounded-lg">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-foreground">Current Access Level</h3>
              <PermissionBadge permissionType={currentPermission} />
            </div>
            <p className="text-sm text-muted-foreground">
              {getPermissionDescription(currentPermission)}
            </p>
          </div>

          {/* Request Form */}
          <form onSubmit={handleSubmit(handleRequestSubmit)} className="space-y-6">
            <div>
              <label htmlFor="requestedPermission" className="label mb-3 block">
                Requested Permission Level *
              </label>
              <select
                {...register('requestedPermission', {
                  required: 'Please select a permission level'
                })}
                id="requestedPermission"
                className="input w-full"
              >
                {availableOptions.map(permission => (
                  <option key={permission} value={permission}>
                    {permission.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                  </option>
                ))}
              </select>
              {errors.requestedPermission && (
                <p className="text-destructive text-sm mt-1">{errors.requestedPermission.message}</p>
              )}
            </div>

            {/* Requested Permission Preview */}
            {requestedPermission && (
              <div className="p-4 bg-muted/30 rounded-lg">
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-semibold text-foreground">Requested Access Level</h4>
                  <PermissionBadge permissionType={requestedPermission} />
                </div>
                <p className="text-sm text-muted-foreground mb-3">
                  {getPermissionDescription(requestedPermission)}
                </p>

                {/* Upgrade Arrow */}
                <div className="flex items-center space-x-3 text-sm">
                  <PermissionBadge permissionType={currentPermission} size="small" />
                  <ArrowUp className="h-4 w-4 text-muted-foreground" />
                  <PermissionBadge permissionType={requestedPermission} size="small" />
                  <span className="text-muted-foreground">Upgrade Request</span>
                </div>
              </div>
            )}

            <div>
              <label htmlFor="reason" className="label mb-2 block">
                Reason for Request *
              </label>
              <textarea
                {...register('reason', {
                  required: 'Please provide a reason for this request',
                  minLength: {
                    value: 10,
                    message: 'Reason must be at least 10 characters'
                  }
                })}
                id="reason"
                rows={4}
                placeholder="Explain why you need this permission level and how you plan to use it..."
                className="input w-full resize-none"
              />
              {errors.reason && (
                <p className="text-destructive text-sm mt-1">{errors.reason.message}</p>
              )}
              <p className="text-xs text-muted-foreground mt-1">
                Be specific about your intended use to help the account owner make a decision.
              </p>
            </div>

            {/* Info Box */}
            <div className="p-4 bg-blue-50 dark:bg-blue-950/50 border border-blue-200 dark:border-blue-800 rounded-lg">
              <div className="flex items-start space-x-2">
                <Info className="h-4 w-4 text-blue-600 dark:text-blue-400 mt-0.5" />
                <div className="text-sm">
                  <p className="font-medium text-blue-900 dark:text-blue-100 mb-1">Request Process</p>
                  <ul className="text-blue-700 dark:text-blue-300 space-y-1 text-xs">
                    <li>• Your request will be sent to the account owner</li>
                    <li>• They will review your reason and current access level</li>
                    <li>• You'll be notified when they approve or deny the request</li>
                    <li>• You can track the status in your permission requests</li>
                  </ul>
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="flex justify-end space-x-3 pt-4">
              <button
                type="button"
                onClick={onClose}
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
                    Submitting...
                  </>
                ) : (
                  <>
                    <Send className="h-4 w-4 mr-2" />
                    Submit Request
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default PermissionRequestDialog;