import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { X, Save, Tag, FileText, Palette, Info } from 'lucide-react';
import { transactionCategoryService } from '../../services/transactionService';

const CategoryForm = ({
  category = null,
  onClose,
  onSubmit,
  mode = 'create' // 'create' or 'edit'
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const isEdit = mode === 'edit' && category;

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors }
  } = useForm({
    defaultValues: {
      name: category?.name || '',
      description: category?.description || '',
      color: category?.color || '#3B82F6',
      isActive: category?.isActive !== undefined ? category.isActive : true
    }
  });

  const watchedColor = watch('color');

  const onFormSubmit = async (data) => {
    try {
      setLoading(true);
      setError('');

      const formData = {
        ...data,
        isActive: Boolean(data.isActive)
      };

      let response;
      if (isEdit) {
        response = await transactionCategoryService.updateCategory(category.id, formData);
      } else {
        response = await transactionCategoryService.createCategory(formData);
      }

      if (response.success) {
        onSubmit(response);
        onClose();
      } else {
        setError(response.message || `Failed to ${isEdit ? 'update' : 'create'} category`);
      }
    } catch (err) {
      console.error(`Error ${isEdit ? 'updating' : 'creating'} category:`, err);
      setError(`Failed to ${isEdit ? 'update' : 'create'} category`);
    } finally {
      setLoading(false);
    }
  };

  const predefinedColors = [
    '#3B82F6', // Blue
    '#10B981', // Green
    '#F59E0B', // Yellow
    '#EF4444', // Red
    '#8B5CF6', // Purple
    '#F97316', // Orange
    '#06B6D4', // Cyan
    '#84CC16', // Lime
    '#EC4899', // Pink
    '#6B7280'  // Gray
  ];

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-lg font-medium text-gray-900">
            {isEdit ? 'Edit Category' : 'Add New Category'}
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

        <form onSubmit={handleSubmit(onFormSubmit)} className="space-y-4">
          {/* Category Name */}
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
              Category Name *
            </label>
            <div className="relative">
              <Tag className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
              <input
                type="text"
                id="name"
                {...register('name', {
                  required: 'Category name is required',
                  maxLength: { value: 100, message: 'Category name must be less than 100 characters' },
                  minLength: { value: 2, message: 'Category name must be at least 2 characters' }
                })}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                placeholder="Enter category name"
              />
            </div>
            {errors.name && (
              <p className="mt-1 text-sm text-red-600">{errors.name.message}</p>
            )}
          </div>

          {/* Description */}
          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <div className="relative">
              <FileText className="absolute left-3 top-3 text-gray-400 h-4 w-4" />
              <textarea
                id="description"
                {...register('description', {
                  maxLength: { value: 500, message: 'Description must be less than 500 characters' }
                })}
                rows={3}
                className="pl-10 w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 resize-none"
                placeholder="Optional description for this category"
              />
            </div>
            {errors.description && (
              <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
            )}
          </div>

          {/* Color Picker */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Category Color
            </label>
            <div className="space-y-3">
              {/* Color Preview */}
              <div className="flex items-center space-x-3">
                <div
                  className="w-8 h-8 rounded-full border-2 border-gray-300"
                  style={{ backgroundColor: watchedColor }}
                ></div>
                <div className="relative flex-1">
                  <Palette className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
                  <input
                    type="color"
                    {...register('color')}
                    className="pl-10 w-full h-10 rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
                  />
                </div>
              </div>

              {/* Predefined Colors */}
              <div>
                <p className="text-xs text-gray-600 mb-2">Quick select:</p>
                <div className="grid grid-cols-5 gap-2">
                  {predefinedColors.map((color) => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => {
                        const event = { target: { name: 'color', value: color } };
                        register('color').onChange(event);
                      }}
                      className={`w-8 h-8 rounded-full border-2 transition-all ${
                        watchedColor === color
                          ? 'border-gray-800 scale-110'
                          : 'border-gray-300 hover:border-gray-500'
                      }`}
                      style={{ backgroundColor: color }}
                      title={`Select ${color}`}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Active Status */}
          <div className="flex items-start space-x-3">
            <input
              {...register('isActive')}
              type="checkbox"
              id="isActive"
              className="mt-1"
            />
            <div>
              <label htmlFor="isActive" className="text-sm font-medium text-gray-700 cursor-pointer">
                Active Category
              </label>
              <p className="text-xs text-gray-600">
                Active categories are available for selection when creating transactions
              </p>
            </div>
          </div>

          {/* Info Box */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <div className="flex items-start space-x-2">
              <Info className="h-4 w-4 text-blue-600 mt-0.5 flex-shrink-0" />
              <div className="text-sm text-blue-700">
                <p className="font-medium mb-1">Category Tips:</p>
                <ul className="text-xs space-y-1">
                  <li>• Use descriptive names like "Groceries" or "Entertainment"</li>
                  <li>• Choose colors to help visually organize your spending</li>
                  <li>• Categories can be edited or deactivated later</li>
                </ul>
              </div>
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
                  {isEdit ? 'Update Category' : 'Create Category'}
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CategoryForm;