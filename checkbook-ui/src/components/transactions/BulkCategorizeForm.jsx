import React, { useState, useEffect } from 'react';
import { X, Tag, Check, AlertCircle } from 'lucide-react';
import { transactionService, transactionCategoryService } from '../../services/transactionService';

const BulkCategorizeForm = ({ transactions = [], onClose, onComplete }) => {
  const [categories, setCategories] = useState([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

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

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!selectedCategoryId) {
      setError('Please select a category');
      return;
    }

    try {
      setLoading(true);
      setError('');

      const transactionIds = transactions.map(t => t.id);
      const response = await transactionService.bulkCategorizeTransactions(
        transactionIds,
        parseInt(selectedCategoryId)
      );

      if (response.success) {
        onComplete(response);
        onClose();
      } else {
        setError(response.message || 'Failed to categorize transactions');
      }
    } catch (err) {
      console.error('Error categorizing transactions:', err);
      setError('Failed to categorize transactions');
    } finally {
      setLoading(false);
    }
  };

  const selectedCategory = categories.find(c => c.id === parseInt(selectedCategoryId));

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-lg shadow-lg rounded-md bg-white">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-medium text-gray-900">
            Bulk Categorize Transactions
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
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded flex items-center">
            <AlertCircle className="h-4 w-4 mr-2" />
            {error}
          </div>
        )}

        {/* Transaction Count */}
        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <div className="flex items-center text-blue-800">
            <Check className="h-5 w-5 mr-2" />
            <span className="font-medium">
              {transactions.length} transaction{transactions.length !== 1 ? 's' : ''} selected
            </span>
          </div>
          <div className="mt-2 text-sm text-blue-600">
            All selected transactions will be assigned to the same category.
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Category Selection */}
          <div>
            <label htmlFor="categoryId" className="block text-sm font-medium text-gray-700 mb-2">
              Select Category *
            </label>
            <div className="relative">
              <Tag className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <select
                id="categoryId"
                value={selectedCategoryId}
                onChange={(e) => setSelectedCategoryId(e.target.value)}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                required
              >
                <option value="">Choose a category...</option>
                {categories.map(category => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Preview */}
          {selectedCategory && (
            <div className="p-4 bg-green-50 border border-green-200 rounded-lg">
              <div className="flex items-center text-green-800">
                <Tag className="h-4 w-4 mr-2" />
                <span className="font-medium">Preview:</span>
              </div>
              <div className="mt-1 text-sm text-green-700">
                All {transactions.length} transaction{transactions.length !== 1 ? 's' : ''} will be categorized as "
                <span className="font-medium">{selectedCategory.name}</span>"
              </div>
            </div>
          )}

          {/* Transaction List Preview */}
          <div className="max-h-48 overflow-y-auto border border-gray-200 rounded-lg">
            <div className="px-4 py-2 bg-gray-50 border-b border-gray-200">
              <span className="text-sm font-medium text-gray-700">
                Transactions to categorize:
              </span>
            </div>
            <div className="divide-y divide-gray-200">
              {transactions.slice(0, 5).map((transaction, index) => (
                <div key={transaction.id} className="px-4 py-3">
                  <div className="flex justify-between items-start">
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-gray-900 truncate">
                        {transaction.description}
                      </div>
                      <div className="text-sm text-gray-500">
                        {new Date(transaction.transactionDate).toLocaleDateString()}
                      </div>
                    </div>
                    <div className="text-sm font-medium">
                      <span className={transaction.type === 'CREDIT' ? 'text-green-600' : 'text-red-600'}>
                        {transaction.type === 'CREDIT' ? '+' : '-'}${Math.abs(transaction.amount).toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
              {transactions.length > 5 && (
                <div className="px-4 py-3 text-center text-sm text-gray-500 bg-gray-50">
                  ... and {transactions.length - 5} more transaction{transactions.length - 5 !== 1 ? 's' : ''}
                </div>
              )}
            </div>
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
              disabled={loading || !selectedCategoryId}
              className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Categorizing...
                </>
              ) : (
                <>
                  <Tag className="h-4 w-4 mr-2" />
                  Categorize Transactions
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BulkCategorizeForm;