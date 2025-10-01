import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, CreditCard, PiggyBank, ShoppingCart, TrendingUp, Wallet, HelpCircle, Save } from 'lucide-react';

const UpdateAccountForm = ({ account, onClose, onSubmit }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isDirty }
  } = useForm({
    defaultValues: {
      name: account?.name || '',
      description: account?.description || '',
      accountType: account?.accountType || 'CHECKING',
      bankName: account?.bankName || '',
      accountNumberMasked: account?.accountNumberMasked || '',
      isShared: account?.isShared || false
    }
  });

  const accountType = watch('accountType');
  const isShared = watch('isShared');

  const accountTypes = [
    { value: 'CHECKING', label: 'Checking Account', icon: CreditCard, description: 'For everyday spending and transactions' },
    { value: 'SAVINGS', label: 'Savings Account', icon: PiggyBank, description: 'For saving money and earning interest' },
    { value: 'CREDIT_CARD', label: 'Credit Card', icon: ShoppingCart, description: 'For credit purchases and building credit' },
    { value: 'INVESTMENT', label: 'Investment Account', icon: TrendingUp, description: 'For stocks, bonds, and other investments' },
    { value: 'CASH', label: 'Cash Account', icon: Wallet, description: 'For tracking cash on hand' },
    { value: 'OTHER', label: 'Other', icon: HelpCircle, description: 'Custom account type' }
  ];

  const getAccountTypeInfo = (type) => {
    return accountTypes.find(t => t.value === type) || accountTypes[0];
  };

  const onFormSubmit = async (data) => {
    try {
      setLoading(true);
      setError('');

      // Only send changed fields
      const formData = {
        ...data
      };

      const result = await onSubmit(formData);

      if (result.success) {
        onClose();
      } else {
        setError(result.message || 'Failed to update account');
      }
    } catch (err) {
      setError('Failed to update account');
      console.error('Error updating account:', err);
    } finally {
      setLoading(false);
    }
  };

  const selectedType = getAccountTypeInfo(accountType);
  const IconComponent = selectedType.icon;

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-10 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-medium text-gray-900">Update Account</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-6">
          {/* Account Name */}
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
              Account Name *
            </label>
            <input
              type="text"
              id="name"
              {...register('name', {
                required: 'Account name is required',
                minLength: { value: 2, message: 'Account name must be at least 2 characters' },
                maxLength: { value: 100, message: 'Account name must be less than 100 characters' }
              })}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              placeholder="e.g., Main Checking, Emergency Savings"
            />
            {errors.name && (
              <p className="mt-1 text-sm text-red-600">{errors.name.message}</p>
            )}
          </div>

          {/* Account Type */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-3">
              Account Type *
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {accountTypes.map((type) => {
                const TypeIcon = type.icon;
                return (
                  <label
                    key={type.value}
                    className={`relative flex items-center p-3 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors ${
                      accountType === type.value
                        ? 'border-blue-500 bg-blue-50 ring-1 ring-blue-500'
                        : 'border-gray-300'
                    }`}
                  >
                    <input
                      type="radio"
                      value={type.value}
                      {...register('accountType', { required: 'Account type is required' })}
                      className="sr-only"
                    />
                    <TypeIcon className={`h-5 w-5 mr-3 ${
                      accountType === type.value ? 'text-blue-600' : 'text-gray-400'
                    }`} />
                    <div className="flex-1">
                      <div className={`text-sm font-medium ${
                        accountType === type.value ? 'text-blue-900' : 'text-gray-900'
                      }`}>
                        {type.label}
                      </div>
                      <div className={`text-xs ${
                        accountType === type.value ? 'text-blue-700' : 'text-gray-500'
                      }`}>
                        {type.description}
                      </div>
                    </div>
                  </label>
                );
              })}
            </div>
            {errors.accountType && (
              <p className="mt-1 text-sm text-red-600">{errors.accountType.message}</p>
            )}
          </div>

          {/* Description */}
          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              id="description"
              {...register('description', {
                maxLength: { value: 500, message: 'Description must be less than 500 characters' }
              })}
              rows={3}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              placeholder="Optional description for this account"
            />
            {errors.description && (
              <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
            )}
          </div>

          {/* Bank Name */}
          <div>
            <label htmlFor="bankName" className="block text-sm font-medium text-gray-700 mb-1">
              Bank/Institution Name
            </label>
            <input
              type="text"
              id="bankName"
              {...register('bankName', {
                maxLength: { value: 100, message: 'Bank name must be less than 100 characters' }
              })}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              placeholder="e.g., Chase, Bank of America"
            />
            {errors.bankName && (
              <p className="mt-1 text-sm text-red-600">{errors.bankName.message}</p>
            )}
          </div>

          {/* Account Number (Masked) */}
          <div>
            <label htmlFor="accountNumberMasked" className="block text-sm font-medium text-gray-700 mb-1">
              Account Number (Last 4 digits)
            </label>
            <input
              type="text"
              id="accountNumberMasked"
              {...register('accountNumberMasked', {
                pattern: {
                  value: /^\d{4}$/,
                  message: 'Please enter exactly 4 digits'
                }
              })}
              maxLength={4}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              placeholder="1234"
            />
            {errors.accountNumberMasked && (
              <p className="mt-1 text-sm text-red-600">{errors.accountNumberMasked.message}</p>
            )}
            <p className="mt-1 text-xs text-gray-500">
              Only the last 4 digits for identification purposes
            </p>
          </div>

          {/* Sharing Options */}
          <div>
            <div className="flex items-center">
              <input
                type="checkbox"
                id="isShared"
                {...register('isShared')}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label htmlFor="isShared" className="ml-2 block text-sm text-gray-900">
                Allow sharing with other users
              </label>
            </div>
            {isShared && (
              <p className="mt-1 text-xs text-blue-600">
                You can manage permissions for this account after updating it.
              </p>
            )}
          </div>

          {/* Account Info Display */}
          <div className="bg-gray-50 rounded-lg p-4">
            <div className="flex items-center mb-2">
              <IconComponent className="h-5 w-5 text-blue-600 mr-2" />
              <span className="font-medium text-gray-900">{selectedType.label}</span>
            </div>
            <p className="text-sm text-gray-600">{selectedType.description}</p>
            <div className="mt-2 text-sm text-gray-600">
              <span className="font-medium">Current Balance:</span> ${account?.currentBalance?.toFixed(2) || '0.00'}
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end space-x-3 pt-4 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || !isDirty}
              className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  Updating...
                </>
              ) : (
                <>
                  <Save className="h-4 w-4 mr-2" />
                  Update Account
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UpdateAccountForm;