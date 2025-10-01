import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, CreditCard, PiggyBank, ShoppingCart, TrendingUp, Wallet, HelpCircle } from 'lucide-react';

const CreateAccountForm = ({ onClose, onSubmit }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors }
  } = useForm({
    defaultValues: {
      name: '',
      description: '',
      accountType: 'CHECKING',
      bankName: '',
      accountNumberMasked: '',
      isShared: false,
      initialBalance: 0
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

      // Convert initialBalance to number (already numeric from number input)
      const formData = {
        ...data,
        initialBalance: data.initialBalance ? Number(data.initialBalance) : 0
      };

      const result = await onSubmit(formData);

      if (result.success) {
        onClose();
      } else {
        setError(result.message || 'Failed to create account');
      }
    } catch (err) {
      setError('An unexpected error occurred');
      console.error('Form submission error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="card max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6">
          {/* Header */}
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-foreground">Create Account</h2>
              <p className="text-muted-foreground">Add a new financial account to your portfolio</p>
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

          <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-6">
            {/* Account Type Selection */}
            <div>
              <label className="label mb-3 block">Account Type</label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {accountTypes.map((type) => {
                  const Icon = type.icon;
                  return (
                    <label
                      key={type.value}
                      className={`relative flex items-start p-4 border rounded-lg cursor-pointer transition-colors ${
                        accountType === type.value
                          ? 'border-primary bg-primary/5'
                          : 'border-border hover:bg-muted/50'
                      }`}
                    >
                      <input
                        {...register('accountType')}
                        type="radio"
                        value={type.value}
                        className="sr-only"
                      />
                      <Icon className={`h-5 w-5 mr-3 mt-0.5 ${
                        accountType === type.value ? 'text-primary' : 'text-muted-foreground'
                      }`} />
                      <div className="flex-1">
                        <div className="font-medium text-foreground">{type.label}</div>
                        <div className="text-sm text-muted-foreground">{type.description}</div>
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Account Name */}
            <div>
              <label htmlFor="name" className="label mb-2 block">
                Account Name *
              </label>
              <input
                {...register('name', {
                  required: 'Account name is required',
                  maxLength: { value: 100, message: 'Account name must be less than 100 characters' }
                })}
                type="text"
                id="name"
                placeholder="e.g., Chase Checking, Emergency Savings"
                className="input w-full"
              />
              {errors.name && (
                <p className="text-destructive text-sm mt-1">{errors.name.message}</p>
              )}
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="label mb-2 block">
                Description
              </label>
              <textarea
                {...register('description', {
                  maxLength: { value: 500, message: 'Description must be less than 500 characters' }
                })}
                id="description"
                rows={3}
                placeholder="Optional description of this account"
                className="input w-full resize-none"
              />
              {errors.description && (
                <p className="text-destructive text-sm mt-1">{errors.description.message}</p>
              )}
            </div>

            {/* Bank Details */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="bankName" className="label mb-2 block">
                  Bank/Institution
                </label>
                <input
                  {...register('bankName', {
                    maxLength: { value: 100, message: 'Bank name must be less than 100 characters' }
                  })}
                  type="text"
                  id="bankName"
                  placeholder="e.g., Chase, Bank of America"
                  className="input w-full"
                />
                {errors.bankName && (
                  <p className="text-destructive text-sm mt-1">{errors.bankName.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="accountNumberMasked" className="label mb-2 block">
                  Account Number (Masked)
                </label>
                <input
                  {...register('accountNumberMasked', {
                    maxLength: { value: 20, message: 'Account number must be less than 20 characters' }
                  })}
                  type="text"
                  id="accountNumberMasked"
                  placeholder="e.g., ***1234"
                  className="input w-full"
                />
                {errors.accountNumberMasked && (
                  <p className="text-destructive text-sm mt-1">{errors.accountNumberMasked.message}</p>
                )}
              </div>
            </div>

            {/* Initial Balance */}
            <div>
              <label htmlFor="initialBalance" className="label mb-2 block">
                Initial Balance
              </label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground">
                  $
                </span>
                <input
                  {...register('initialBalance', {
                    min: { value: -999999999999.99, message: 'Balance must be valid' },
                    max: { value: 999999999999.99, message: 'Balance must be valid' }
                  })}
                  type="number"
                  step="0.01"
                  id="initialBalance"
                  placeholder="0.00"
                  className="input w-full pl-8"
                />
              </div>
              {errors.initialBalance && (
                <p className="text-destructive text-sm mt-1">{errors.initialBalance.message}</p>
              )}
            </div>

            {/* Sharing Options */}
            <div className="space-y-4">
              <div className="flex items-start space-x-3">
                <input
                  {...register('isShared')}
                  type="checkbox"
                  id="isShared"
                  className="mt-1"
                />
                <div>
                  <label htmlFor="isShared" className="label cursor-pointer">
                    Enable sharing for this account
                  </label>
                  <p className="text-sm text-muted-foreground">
                    Allow other users to view or manage this account with permissions
                  </p>
                </div>
              </div>

              {isShared && (
                <div className="p-4 bg-muted/30 rounded-lg">
                  <div className="flex items-start space-x-2">
                    <div className="h-2 w-2 bg-primary rounded-full mt-2"></div>
                    <div className="text-sm text-muted-foreground">
                      <p className="font-medium text-foreground mb-1">Sharing Features:</p>
                      <ul className="space-y-1">
                        <li>• Grant view-only or full access to other users</li>
                        <li>• Manage permissions after account creation</li>
                        <li>• You maintain full ownership and control</li>
                      </ul>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Action Buttons */}
            <div className="flex justify-end space-x-3 pt-4 border-t border-border">
              <button
                type="button"
                onClick={onClose}
                className="btn btn-outline"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                    Creating...
                  </>
                ) : (
                  <>
                    <CreditCard className="h-4 w-4 mr-2" />
                    Create Account
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

export default CreateAccountForm;