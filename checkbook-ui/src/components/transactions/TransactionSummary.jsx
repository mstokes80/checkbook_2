import React, { useState, useEffect, useMemo } from 'react';
import {
  Calendar,
  Download,
  TrendingUp,
  TrendingDown,
  PieChart,
  BarChart3,
  DollarSign,
  Tag,
  RefreshCw,
  Filter,
  FileDown
} from 'lucide-react';
import { transactionService, transactionCategoryService } from '../../services/transactionService';

const TransactionSummary = ({ accountId, onClose }) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [statistics, setStatistics] = useState(null);

  // Date range state
  const [startDate, setStartDate] = useState(() => {
    const date = new Date();
    date.setMonth(date.getMonth() - 1); // Default to last month
    return date.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });

  // View state
  const [viewMode, setViewMode] = useState('overview'); // 'overview', 'categories', 'trends'

  useEffect(() => {
    if (accountId) {
      loadData();
    }
  }, [accountId, startDate, endDate]);

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');

      // Load transactions, categories, and statistics in parallel
      const [transactionsResponse, categoriesResponse, statisticsResponse] = await Promise.all([
        transactionService.getAccountTransactions({
          accountId,
          startDate,
          endDate,
          size: 1000 // Get all transactions for analysis
        }),
        transactionCategoryService.getAllCategories(),
        transactionService.getTransactionStatistics({
          accountId,
          startDate,
          endDate
        })
      ]);

      if (transactionsResponse.success) {
        setTransactions(transactionsResponse.data.content || []);
      }

      if (categoriesResponse.success) {
        setCategories(categoriesResponse.data || []);
      }

      if (statisticsResponse.success) {
        setStatistics(statisticsResponse.data || {});
      }

    } catch (err) {
      console.error('Error loading summary data:', err);
      setError('Failed to load transaction summary data');
    } finally {
      setLoading(false);
    }
  };

  // Calculate summary data
  const summaryData = useMemo(() => {
    if (!transactions.length) {
      return {
        totalIncome: 0,
        totalExpenses: 0,
        netAmount: 0,
        transactionCount: 0,
        categoryBreakdown: [],
        dailyTrends: []
      };
    }

    const income = transactions
      .filter(t => t.amount > 0)
      .reduce((sum, t) => sum + t.amount, 0);

    const expenses = transactions
      .filter(t => t.amount < 0)
      .reduce((sum, t) => sum + Math.abs(t.amount), 0);

    // Category breakdown
    const categoryMap = new Map();
    categories.forEach(cat => {
      categoryMap.set(cat.id, { ...cat, income: 0, expenses: 0, count: 0 });
    });

    // Add uncategorized
    categoryMap.set(null, {
      id: null,
      name: 'Uncategorized',
      income: 0,
      expenses: 0,
      count: 0
    });

    transactions.forEach(transaction => {
      const categoryId = transaction.categoryId;
      const category = categoryMap.get(categoryId);
      if (category) {
        if (transaction.amount > 0) {
          category.income += transaction.amount;
        } else {
          category.expenses += Math.abs(transaction.amount);
        }
        category.count += 1;
      }
    });

    const categoryBreakdown = Array.from(categoryMap.values())
      .filter(cat => cat.count > 0)
      .sort((a, b) => (b.income + b.expenses) - (a.income + a.expenses));

    // Daily trends (group by date)
    const dailyMap = new Map();
    transactions.forEach(transaction => {
      const date = transaction.transactionDate.split('T')[0];
      if (!dailyMap.has(date)) {
        dailyMap.set(date, { date, income: 0, expenses: 0, count: 0 });
      }
      const day = dailyMap.get(date);
      if (transaction.amount > 0) {
        day.income += transaction.amount;
      } else {
        day.expenses += Math.abs(transaction.amount);
      }
      day.count += 1;
    });

    const dailyTrends = Array.from(dailyMap.values())
      .sort((a, b) => a.date.localeCompare(b.date));

    return {
      totalIncome: income,
      totalExpenses: expenses,
      netAmount: income - expenses,
      transactionCount: transactions.length,
      categoryBreakdown,
      dailyTrends
    };
  }, [transactions, categories]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric'
    });
  };

  const handleExport = () => {
    try {
      // Create CSV content
      const headers = ['Date', 'Description', 'Amount', 'Type', 'Category'];
      const csvContent = [
        headers.join(','),
        ...transactions.map(t => [
          t.transactionDate,
          `"${t.description}"`,
          t.amount,
          t.type,
          `"${categories.find(c => c.id === t.categoryId)?.name || 'Uncategorized'}"`
        ].join(','))
      ].join('\n');

      // Create and download file
      const blob = new Blob([csvContent], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `transaction-summary-${startDate}-to-${endDate}.csv`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error exporting data:', err);
    }
  };

  if (!accountId) {
    return (
      <div className="bg-white rounded-lg shadow p-6">
        <div className="text-center text-gray-500">
          Select an account to view transaction summary
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow">
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex justify-between items-start">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Transaction Summary</h2>
            <p className="text-sm text-gray-600 mt-1">
              Analysis for {formatDate(startDate)} - {formatDate(endDate)}
            </p>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={handleExport}
              className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <FileDown className="h-4 w-4 mr-2" />
              Export
            </button>
            <button
              onClick={loadData}
              className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              Refresh
            </button>
          </div>
        </div>

        {/* Date Range Controls */}
        <div className="mt-4 flex flex-col sm:flex-row gap-4">
          <div className="flex items-center space-x-2">
            <label className="text-sm font-medium text-gray-700">From:</label>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-sm"
            />
          </div>
          <div className="flex items-center space-x-2">
            <label className="text-sm font-medium text-gray-700">To:</label>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 text-sm"
            />
          </div>
        </div>

        {/* View Mode Tabs */}
        <div className="mt-4 border-b border-gray-200">
          <nav className="-mb-px flex space-x-8">
            {[
              { id: 'overview', name: 'Overview', icon: BarChart3 },
              { id: 'categories', name: 'Categories', icon: PieChart },
              { id: 'trends', name: 'Trends', icon: TrendingUp }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setViewMode(tab.id)}
                className={`py-2 px-1 border-b-2 font-medium text-sm flex items-center ${
                  viewMode === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <tab.icon className="h-4 w-4 mr-2" />
                {tab.name}
              </button>
            ))}
          </nav>
        </div>
      </div>

      {/* Content */}
      <div className="p-6">
        {loading ? (
          <div className="flex items-center justify-center h-32">
            <div className="text-center">
              <RefreshCw className="animate-spin h-8 w-8 text-blue-500 mx-auto mb-2" />
              <p className="text-gray-600">Loading summary...</p>
            </div>
          </div>
        ) : error ? (
          <div className="text-center text-red-600 py-8">
            {error}
          </div>
        ) : (
          <>
            {viewMode === 'overview' && (
              <div className="space-y-6">
                {/* Summary Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                  <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                    <div className="flex items-center">
                      <TrendingUp className="h-8 w-8 text-green-600" />
                      <div className="ml-3">
                        <p className="text-sm font-medium text-green-800">Total Income</p>
                        <p className="text-lg font-semibold text-green-900">
                          {formatCurrency(summaryData.totalIncome)}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                    <div className="flex items-center">
                      <TrendingDown className="h-8 w-8 text-red-600" />
                      <div className="ml-3">
                        <p className="text-sm font-medium text-red-800">Total Expenses</p>
                        <p className="text-lg font-semibold text-red-900">
                          {formatCurrency(summaryData.totalExpenses)}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className={`border rounded-lg p-4 ${
                    summaryData.netAmount >= 0
                      ? 'bg-blue-50 border-blue-200'
                      : 'bg-orange-50 border-orange-200'
                  }`}>
                    <div className="flex items-center">
                      <DollarSign className={`h-8 w-8 ${
                        summaryData.netAmount >= 0 ? 'text-blue-600' : 'text-orange-600'
                      }`} />
                      <div className="ml-3">
                        <p className={`text-sm font-medium ${
                          summaryData.netAmount >= 0 ? 'text-blue-800' : 'text-orange-800'
                        }`}>
                          Net Amount
                        </p>
                        <p className={`text-lg font-semibold ${
                          summaryData.netAmount >= 0 ? 'text-blue-900' : 'text-orange-900'
                        }`}>
                          {formatCurrency(summaryData.netAmount)}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center">
                      <Tag className="h-8 w-8 text-gray-600" />
                      <div className="ml-3">
                        <p className="text-sm font-medium text-gray-800">Transactions</p>
                        <p className="text-lg font-semibold text-gray-900">
                          {summaryData.transactionCount}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Quick Stats */}
                {summaryData.transactionCount > 0 && (
                  <div className="bg-gray-50 rounded-lg p-4">
                    <h3 className="text-lg font-medium text-gray-900 mb-3">Quick Stats</h3>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                      <div>
                        <p className="text-gray-600">Avg Income/Transaction</p>
                        <p className="font-semibold text-gray-900">
                          {formatCurrency(summaryData.totalIncome / Math.max(1, transactions.filter(t => t.amount > 0).length))}
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-600">Avg Expense/Transaction</p>
                        <p className="font-semibold text-gray-900">
                          {formatCurrency(summaryData.totalExpenses / Math.max(1, transactions.filter(t => t.amount < 0).length))}
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-600">Income Ratio</p>
                        <p className="font-semibold text-gray-900">
                          {((summaryData.totalIncome / (summaryData.totalIncome + summaryData.totalExpenses)) * 100 || 0).toFixed(1)}%
                        </p>
                      </div>
                      <div>
                        <p className="text-gray-600">Expense Ratio</p>
                        <p className="font-semibold text-gray-900">
                          {((summaryData.totalExpenses / (summaryData.totalIncome + summaryData.totalExpenses)) * 100 || 0).toFixed(1)}%
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {viewMode === 'categories' && (
              <div className="space-y-6">
                <h3 className="text-lg font-medium text-gray-900">Category Breakdown</h3>
                {summaryData.categoryBreakdown.length > 0 ? (
                  <div className="space-y-3">
                    {summaryData.categoryBreakdown.map((category) => {
                      const totalAmount = category.income + category.expenses;
                      const maxAmount = Math.max(...summaryData.categoryBreakdown.map(c => c.income + c.expenses));
                      const percentage = maxAmount > 0 ? (totalAmount / maxAmount) * 100 : 0;

                      return (
                        <div key={category.id || 'uncategorized'} className="border border-gray-200 rounded-lg p-4">
                          <div className="flex justify-between items-start mb-2">
                            <div>
                              <h4 className="font-medium text-gray-900">{category.name}</h4>
                              <p className="text-sm text-gray-600">{category.count} transactions</p>
                            </div>
                            <div className="text-right">
                              <p className="font-semibold text-gray-900">
                                {formatCurrency(totalAmount)}
                              </p>
                            </div>
                          </div>

                          <div className="grid grid-cols-2 gap-4 mb-3">
                            {category.income > 0 && (
                              <div className="text-sm">
                                <span className="text-green-600">Income: </span>
                                <span className="font-medium">{formatCurrency(category.income)}</span>
                              </div>
                            )}
                            {category.expenses > 0 && (
                              <div className="text-sm">
                                <span className="text-red-600">Expenses: </span>
                                <span className="font-medium">{formatCurrency(category.expenses)}</span>
                              </div>
                            )}
                          </div>

                          {/* Progress bar */}
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div
                              className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                              style={{ width: `${percentage}%` }}
                            ></div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-gray-500 text-center py-8">No category data available</p>
                )}
              </div>
            )}

            {viewMode === 'trends' && (
              <div className="space-y-6">
                <h3 className="text-lg font-medium text-gray-900">Daily Trends</h3>
                {summaryData.dailyTrends.length > 0 ? (
                  <div className="space-y-2">
                    {summaryData.dailyTrends.map((day) => {
                      const netAmount = day.income - day.expenses;
                      return (
                        <div key={day.date} className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
                          <div className="flex-1">
                            <p className="font-medium text-gray-900">{formatDate(day.date)}</p>
                            <p className="text-sm text-gray-600">{day.count} transactions</p>
                          </div>
                          <div className="flex items-center space-x-4">
                            {day.income > 0 && (
                              <div className="text-sm">
                                <span className="text-green-600">+{formatCurrency(day.income)}</span>
                              </div>
                            )}
                            {day.expenses > 0 && (
                              <div className="text-sm">
                                <span className="text-red-600">-{formatCurrency(day.expenses)}</span>
                              </div>
                            )}
                            <div className={`text-sm font-medium ${
                              netAmount >= 0 ? 'text-blue-600' : 'text-orange-600'
                            }`}>
                              {netAmount >= 0 ? '+' : ''}{formatCurrency(netAmount)}
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-gray-500 text-center py-8">No trend data available</p>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default TransactionSummary;