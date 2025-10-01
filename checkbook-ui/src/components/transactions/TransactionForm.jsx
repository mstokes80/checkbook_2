import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { X, Save, Calendar, DollarSign, FileText, Tag, Plus, Minus } from 'lucide-react';
import { transactionService, transactionCategoryService } from '../../services/transactionService';

const TransactionForm = ({
  transaction = null,
  accountId,
  onClose,
  onSubmit,
  mode = 'create' // 'create' or 'edit'
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [categories, setCategories] = useState([]);

  const isEdit = mode === 'edit' && transaction;

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors }
  } = useForm({
    defaultValues: {
      accountId: accountId || transaction?.accountId || '',
      description: transaction?.description || '',
      amount: transaction ? Math.abs(transaction.amount).toString() : '',
      type: transaction?.type || 'DEBIT',
      transactionDate: transaction?.transactionDate || new Date().toISOString().split('T')[0],
      categoryId: transaction?.categoryId || '',
      note: transaction?.note || ''
    }
  });

  const watchedType = watch('type');
  const watchedAmount = watch('amount');

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      const response = await transactionCategoryService.getAllCategories();
      if (response.success) {
        setCategories(response.data || []);
      }
    } catch (err) {
      console.error('Error loading categories:', err);
    }
  };

  const onFormSubmit = async (data) => {
    try {
      setLoading(true);
      setError('');

      // Convert amount to number and apply sign based on type
      const amount = parseFloat(data.amount) || 0;
      const signedAmount = data.type === 'CREDIT' ? amount : -amount;

      const formData = {
        ...data,
        amount: signedAmount,
        accountId: parseInt(data.accountId),
        categoryId: data.categoryId ? parseInt(data.categoryId) : null
      };

      let response;
      if (isEdit) {
        response = await transactionService.updateTransaction(transaction.id, formData);
      } else {
        response = await transactionService.createTransaction(formData);
      }

      if (response.success) {
        onSubmit(response);
        onClose();
      } else {
        setError(response.message || `Failed to ${isEdit ? 'update' : 'create'} transaction`);
      }
    } catch (err) {
      console.error(`Error ${isEdit ? 'updating' : 'creating'} transaction:`, err);
      setError(`Failed to ${isEdit ? 'update' : 'create'} transaction`);
    } finally {
      setLoading(false);
    }
  };

  const handleTypeToggle = (type) => {
    setValue('type', type);
  };

  const getFormattedAmount = () => {
    const amount = parseFloat(watchedAmount) || 0;
    if (amount === 0) return '';
    return watchedType === 'CREDIT' ? `+$${amount.toFixed(2)}` : `-$${amount.toFixed(2)}`;
  };

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-medium text-gray-900">
            {isEdit ? 'Edit Transaction' : 'Add New Transaction'}
          </h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-6">
          {/* Transaction Type Toggle */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Transaction Type
            </label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => handleTypeToggle('DEBIT')}
                className={`flex items-center justify-center px-4 py-3 border rounded-lg text-sm font-medium transition-colors ${
                  watchedType === 'DEBIT'
                    ? 'border-red-500 bg-red-50 text-red-700'
                    : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Minus className="h-4 w-4 mr-2" />
                Expense (Debit)
              </button>
              <button
                type="button"
                onClick={() => handleTypeToggle('CREDIT')}
                className={`flex items-center justify-center px-4 py-3 border rounded-lg text-sm font-medium transition-colors ${
                  watchedType === 'CREDIT'
                    ? 'border-green-500 bg-green-50 text-green-700'
                    : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Plus className="h-4 w-4 mr-2" />
                Income (Credit)
              </button>
            </div>
          </div>

          {/* Description */}
          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description *
            </label>
            <div className="relative">
              <FileText className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <input
                type="text"
                id="description"
                {...register('description', {
                  required: 'Description is required',
                  maxLength: { value: 255, message: 'Description must be less than 255 characters' }
                })}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                placeholder="Enter transaction description"
              />
            </div>
            {errors.description && (
              <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
            )}
          </div>

          {/* Amount */}
          <div>
            <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-1">
              Amount *
            </label>
            <div className="relative">
              <DollarSign className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <input
                type="number"
                id="amount"
                step="0.01"
                min="0"
                {...register('amount', {
                  required: 'Amount is required',
                  min: { value: 0.01, message: 'Amount must be greater than 0' }
                })}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                placeholder="0.00"
              />
              {getFormattedAmount() && (
                <div className="absolute right-3 top-1/2 transform -translate-y-1/2 text-sm font-medium">
                  {getFormattedAmount()}
                </div>
              )}
            </div>
            {errors.amount && (
              <p className="mt-1 text-sm text-red-600">{errors.amount.message}</p>
            )}
          </div>

          {/* Date */}
          <div>
            <label htmlFor="transactionDate" className="block text-sm font-medium text-gray-700 mb-1">
              Transaction Date *
            </label>
            <div className="relative">
              <Calendar className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <input
                type="date"
                id="transactionDate"
                {...register('transactionDate', { required: 'Transaction date is required' })}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              />
            </div>
            {errors.transactionDate && (
              <p className="mt-1 text-sm text-red-600">{errors.transactionDate.message}</p>
            )}
          </div>

          {/* Category */}
          <div>
            <label htmlFor="categoryId" className="block text-sm font-medium text-gray-700 mb-1">
              Category
            </label>
            <div className="relative">
              <Tag className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <select
                id="categoryId"
                {...register('categoryId')}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              >
                <option value="">Select a category (optional)</option>
                {categories.map(category => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Note */}
          <div>
            <label htmlFor="note" className="block text-sm font-medium text-gray-700 mb-1">
              Note
            </label>
            <textarea
              id="note"
              {...register('note', {
                maxLength: { value: 500, message: 'Note must be less than 500 characters' }
              })}
              rows={3}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              placeholder="Add any additional notes (optional)"
            />
            {errors.note && (
              <p className="mt-1 text-sm text-red-600">{errors.note.message}</p>
            )}
          </div>

          {/* Form Actions */}
          <div className="flex justify-end space-x-3 pt-6 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {isEdit ? 'Updating...' : 'Creating...'}
                </>
              ) : (
                <>
                  <Save className="h-4 w-4 mr-2" />
                  {isEdit ? 'Update Transaction' : 'Create Transaction'}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default TransactionForm;