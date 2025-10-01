import React, { useState, useEffect } from 'react';
import {
  Tag,
  Plus,
  Edit,
  Trash2,
  Search,
  RefreshCw,
  Eye,
  EyeOff,
  MoreVertical
} from 'lucide-react';
import { transactionCategoryService } from '../../services/transactionService';
import CategoryForm from './CategoryForm';

const CategoryManager = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [showInactive, setShowInactive] = useState(false);

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      setLoading(true);
      setError('');

      console.log('Loading categories...');
      const response = await transactionCategoryService.getAllCategories();
      console.log('Categories API response:', response);

      if (response.success) {
        console.log('Categories loaded successfully:', response.data);
        setCategories(response.data || []);
      } else {
        console.error('Categories API error:', response);
        setError(response.message || 'Failed to load categories');
      }
    } catch (err) {
      console.error('Error loading categories:', err);
      console.error('Error details:', err.response?.data);
      console.error('Error status:', err.response?.status);
      setError('Failed to load categories: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleCreateCategory = () => {
    setEditingCategory(null);
    setShowForm(true);
  };

  const handleEditCategory = (category) => {
    setEditingCategory(category);
    setShowForm(true);
  };

  const handleFormSubmit = (response) => {
    loadCategories(); // Reload the categories list
  };

  const handleFormClose = () => {
    setShowForm(false);
    setEditingCategory(null);
  };

  const handleDeleteCategory = async (category) => {
    if (!confirm(`Are you sure you want to delete the category "${category.name}"?`)) {
      return;
    }

    try {
      const response = await transactionCategoryService.deleteCategory(category.id);

      if (response.success) {
        loadCategories(); // Reload the categories list
      } else {
        alert(response.message || 'Failed to delete category');
      }
    } catch (err) {
      console.error('Error deleting category:', err);
      alert('Failed to delete category');
    }
  };

  const toggleCategoryStatus = async (category) => {
    try {
      const updatedCategory = {
        ...category,
        isActive: !category.isActive
      };

      const response = await transactionCategoryService.updateCategory(category.id, updatedCategory);

      if (response.success) {
        loadCategories(); // Reload the categories list
      } else {
        alert(response.message || 'Failed to update category');
      }
    } catch (err) {
      console.error('Error updating category:', err);
      alert('Failed to update category');
    }
  };

  const filteredCategories = categories.filter(category => {
    const matchesSearch = category.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (category.description && category.description.toLowerCase().includes(searchTerm.toLowerCase()));

    // Filter by system vs user categories
    const matchesSystemFilter = showInactive ? true : !category.isSystemDefault;

    return matchesSearch && matchesSystemFilter;
  });

  console.log('Categories state:', categories);
  console.log('Filtered categories:', filteredCategories);
  console.log('Search term:', searchTerm);
  console.log('Show inactive:', showInactive);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
          <div className="flex items-center justify-center h-32">
            <div className="text-center">
              <RefreshCw className="animate-spin h-8 w-8 text-blue-600 mx-auto mb-2" />
              <p className="text-gray-600">Loading categories...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
        <div className="mb-6">
          <div className="mb-4">
            <h1 className="text-3xl font-bold text-gray-900">Categories</h1>
            <p className="text-gray-600">
              Manage your transaction categories
            </p>
          </div>
          <div className="flex items-center space-x-3">
            <button
              onClick={loadCategories}
              className="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh
            </button>
            <button
              onClick={handleCreateCategory}
              className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Category
            </button>
          </div>
        </div>

        {/* Controls */}
        <div className="flex flex-col sm:flex-row gap-4">
          {/* Search */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-4 w-4" />
            <input
              type="text"
              placeholder="Search categories..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 pl-10"
            />
          </div>

          {/* Filter by System vs User Categories */}
          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              id="showSystemCategories"
              checked={showInactive}
              onChange={(e) => setShowInactive(e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="showSystemCategories" className="text-sm text-gray-900">
              Show system categories
            </label>
          </div>
        </div>

        {/* Results Count */}
        <div className="mt-4 text-sm text-gray-600">
          Showing {filteredCategories.length} of {categories.length} categories
        </div>
      </div>

      {/* Content */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-lg">
            {error}
          </div>
        )}

        {filteredCategories.length === 0 ? (
          <div className="text-center py-12">
            {searchTerm ? (
              <div>
                <Search className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-600">No categories found matching "{searchTerm}"</p>
              </div>
            ) : (
              <div>
                <Tag className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-600 mb-4">No categories yet</p>
                <button
                  onClick={handleCreateCategory}
                  className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Create your first category
                </button>
              </div>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredCategories.map((category) => (
              <div
                key={category.id}
                className={`bg-white rounded-lg border shadow-sm p-6 hover:shadow-lg transition-shadow ${
                  category.isSystemDefault
                    ? 'border-blue-200 bg-blue-50'
                    : 'border-gray-200'
                }`}
              >
                {/* Category Header */}
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center space-x-3 flex-1 min-w-0">
                    <div
                      className="w-5 h-5 rounded-full flex-shrink-0"
                      style={{ backgroundColor: category.color || '#3B82F6' }}
                    />
                    <div className="min-w-0 flex-1">
                      <h3 className="font-semibold text-gray-900 truncate">
                        {category.displayName || category.name}
                      </h3>
                      {category.isSystemDefault && (
                        <span className="text-xs text-blue-600 font-medium">System Category</span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Description */}
                {category.description && (
                  <p className="text-sm text-gray-600 mb-4 line-clamp-2">
                    {category.description}
                  </p>
                )}

                {/* Category Stats */}
                <div className="mb-4 p-3 bg-gray-50 rounded-lg">
                  <div className="text-sm text-gray-600">
                    <span className="font-medium text-gray-900">{category.transactionCount || 0}</span> transactions
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center justify-between pt-4 border-t border-gray-200">
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => handleEditCategory(category)}
                      className="inline-flex items-center p-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                      title="Edit category"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => toggleCategoryStatus(category)}
                      className={`inline-flex items-center p-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium bg-white focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 ${
                        category.isActive
                          ? 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                          : 'text-green-600 hover:text-green-700 hover:bg-green-50'
                      }`}
                      title={category.isActive ? 'Deactivate category' : 'Activate category'}
                    >
                      {category.isActive ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                    <button
                      onClick={() => handleDeleteCategory(category)}
                      className="inline-flex items-center p-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-red-600 bg-white hover:bg-red-50 hover:text-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                      title="Delete category"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Category Form Modal */}
      {showForm && (
        <CategoryForm
          category={editingCategory}
          mode={editingCategory ? 'edit' : 'create'}
          onClose={handleFormClose}
          onSubmit={handleFormSubmit}
        />
      )}
    </div>
  );
};

export default CategoryManager;