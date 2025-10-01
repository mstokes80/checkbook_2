import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { ROUTES } from '../utils/constants';
import { Link } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { accountService } from '../services/accountService';
import { transactionService } from '../services/transactionService';
import AccountDashboard from '../components/accounts/AccountDashboard';
import CategoryManager from '../components/transactions/CategoryManager';

const Dashboard = () => {
  const { user, logout } = useAuth();
  const [currentView, setCurrentView] = useState('overview');
  const [dashboardData, setDashboardData] = useState(null);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (currentView === 'overview') {
      loadDashboardData();
    }
  }, [currentView]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError('');

      // Load accounts first
      const accountsResponse = await accountService.getAccounts();
      console.log('Accounts response:', accountsResponse);

      if (accountsResponse.success && accountsResponse.data) {
        const accounts = accountsResponse.data;

        // Calculate dashboard metrics from accounts data
        const totalBalance = accounts.reduce((sum, account) => sum + (account.currentBalance || 0), 0);
        const totalAccounts = accounts.length;

        // Try to get dashboard data from API, but fall back to calculated values
        try {
          const dashboardResponse = await accountService.getDashboardData();
          console.log('Dashboard response:', dashboardResponse);

          if (dashboardResponse.success && dashboardResponse.data) {
            setDashboardData({
              totalBalance: dashboardResponse.data.totalBalance || totalBalance,
              totalTransactions: dashboardResponse.data.totalTransactions || 0,
              totalAccounts: dashboardResponse.data.totalAccounts || totalAccounts
            });
          } else {
            // Fallback to calculated values
            setDashboardData({
              totalBalance,
              totalTransactions: 0, // Will be updated if we can get transaction data
              totalAccounts
            });
          }
        } catch (dashboardErr) {
          console.warn('Dashboard API call failed, using calculated values:', dashboardErr);
          setDashboardData({
            totalBalance,
            totalTransactions: 0,
            totalAccounts
          });
        }

        // Get recent transactions and calculate total transaction count
        let totalTransactionCount = 0;
        let allRecentTransactions = [];

        if (accounts.length > 0) {
          try {
            // Get transactions from each account and aggregate
            const transactionPromises = accounts.map(account =>
              transactionService.getAccountTransactions({
                accountId: account.id,
                page: 0,
                size: 5,
                sort: 'transactionDate',
                direction: 'desc'
              }).catch(err => {
                console.error(`Error loading transactions for account ${account.id}:`, err);
                return { success: false, data: { content: [], totalElements: 0 } };
              })
            );

            const transactionResponses = await Promise.all(transactionPromises);

            // Calculate total transaction count and collect recent transactions
            transactionResponses.forEach(response => {
              if (response.success && response.data) {
                totalTransactionCount += response.data.totalElements || 0;
                if (response.data.content) {
                  allRecentTransactions.push(...response.data.content);
                }
              }
            });

            // Sort all recent transactions by date and take the most recent 5
            allRecentTransactions.sort((a, b) => new Date(b.transactionDate) - new Date(a.transactionDate));
            setRecentTransactions(allRecentTransactions.slice(0, 5));

            // Update dashboard data with actual transaction count
            setDashboardData(prev => ({
              ...prev,
              totalTransactions: totalTransactionCount
            }));

          } catch (err) {
            console.error('Error loading transactions:', err);
          }
        }
      } else {
        setError(accountsResponse.message || 'Failed to load accounts data');
      }
    } catch (err) {
      console.error('Error loading dashboard data:', err);
      setError('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(Math.abs(amount || 0));
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 1) return 'Yesterday';
    if (diffDays <= 7) return `${diffDays} days ago`;
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  const getCurrentMonthYear = () => {
    const now = new Date();
    return now.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  const handleNavClick = (view) => {
    setCurrentView(view);
    setIsMobileMenuOpen(false);
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-card border-b border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div>
              <h1 className="text-2xl font-bold text-foreground">Checkbook</h1>
              <p className="text-sm text-muted-foreground">
                Welcome back, {user?.fullName}!
              </p>
            </div>
            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center space-x-4">
              <button
                onClick={() => handleNavClick('overview')}
                className={`text-sm font-medium transition-colors ${
                  currentView === 'overview'
                    ? 'text-foreground'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                Overview
              </button>
              <button
                onClick={() => handleNavClick('accounts')}
                className={`text-sm font-medium transition-colors ${
                  currentView === 'accounts'
                    ? 'text-foreground'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                Accounts
              </button>
              <button
                onClick={() => handleNavClick('categories')}
                className={`text-sm font-medium transition-colors ${
                  currentView === 'categories'
                    ? 'text-foreground'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                Categories
              </button>
              <Link
                to={ROUTES.REPORTS}
                className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
              >
                Reports
              </Link>
              <Link
                to={ROUTES.PROFILE}
                className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
              >
                Profile
              </Link>
              <button
                onClick={handleLogout}
                className="btn btn-outline"
              >
                Sign out
              </button>
            </nav>

            {/* Mobile Hamburger Button */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden p-2 rounded-md text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
              aria-label="Toggle navigation menu"
            >
              {isMobileMenuOpen ? (
                <X className="h-6 w-6" />
              ) : (
                <Menu className="h-6 w-6" />
              )}
            </button>
          </div>
        </div>

        {/* Mobile Navigation Menu */}
        {isMobileMenuOpen && (
          <div className="md:hidden border-t border-border">
            <div className="px-4 py-2 space-y-1 bg-card">
              <button
                onClick={() => handleNavClick('overview')}
                className={`block w-full text-left px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  currentView === 'overview'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                }`}
              >
                Overview
              </button>
              <button
                onClick={() => handleNavClick('accounts')}
                className={`block w-full text-left px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  currentView === 'accounts'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                }`}
              >
                Accounts
              </button>
              <button
                onClick={() => handleNavClick('categories')}
                className={`block w-full text-left px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  currentView === 'categories'
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                }`}
              >
                Categories
              </button>
              <Link
                to={ROUTES.REPORTS}
                onClick={() => setIsMobileMenuOpen(false)}
                className="block w-full text-left px-3 py-2 rounded-md text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
              >
                Reports
              </Link>
              <Link
                to={ROUTES.PROFILE}
                onClick={() => setIsMobileMenuOpen(false)}
                className="block w-full text-left px-3 py-2 rounded-md text-sm font-medium text-muted-foreground hover:text-foreground hover:bg-muted transition-colors"
              >
                Profile
              </Link>
              <button
                onClick={() => {
                  setIsMobileMenuOpen(false);
                  handleLogout();
                }}
                className="block w-full text-left px-3 py-2 rounded-md text-sm font-medium text-destructive hover:bg-destructive hover:text-destructive-foreground transition-colors"
              >
                Sign out
              </button>
            </div>
          </div>
        )}
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {currentView === 'overview' ? (
          <>
            {error && (
              <div className="mb-6 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                {error}
              </div>
            )}

            {loading ? (
              <div className="flex items-center justify-center h-64">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-2"></div>
                  <p className="text-muted-foreground">Loading dashboard...</p>
                </div>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  <div className="card p-6">
                    <h3 className="text-lg font-semibold text-card-foreground mb-2">
                      Total Balance
                    </h3>
                    <p className="text-3xl font-bold text-primary">
                      {dashboardData?.totalBalance ? formatCurrency(dashboardData.totalBalance) : '$0.00'}
                    </p>
                    <p className="text-sm text-muted-foreground mt-1">
                      Across all accounts
                    </p>
                  </div>

                  <div className="card p-6">
                    <h3 className="text-lg font-semibold text-card-foreground mb-2">
                      Total Transactions
                    </h3>
                    <p className="text-2xl font-bold text-foreground">
                      {dashboardData?.totalTransactions || 0}
                    </p>
                    <p className="text-sm text-muted-foreground mt-1">
                      All time
                    </p>
                  </div>

                  <div className="card p-6">
                    <h3 className="text-lg font-semibold text-card-foreground mb-2">
                      Total Accounts
                    </h3>
                    <p className="text-2xl font-bold text-foreground">
                      {dashboardData?.totalAccounts || 0}
                    </p>
                    <p className="text-sm text-muted-foreground mt-1">
                      Active accounts
                    </p>
                  </div>
                </div>
              </>
            )}

            <div className="mt-8">
              <div className="card p-6">
                <h2 className="text-xl font-semibold text-card-foreground mb-4">
                  Quick Actions
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  <button className="btn btn-primary">
                    Add Transaction
                  </button>
                  <button
                    onClick={() => setCurrentView('accounts')}
                    className="btn btn-secondary"
                  >
                    View Accounts
                  </button>
                  <button
                    onClick={() => setCurrentView('categories')}
                    className="btn btn-outline"
                  >
                    Manage Categories
                  </button>
                  <button className="btn btn-outline">
                    Import Transactions
                  </button>
                </div>
              </div>
            </div>

            {!loading && (
              <div className="mt-8">
                <div className="card p-6">
                  <h2 className="text-xl font-semibold text-card-foreground mb-4">
                    Recent Activity
                  </h2>
                  {recentTransactions.length > 0 ? (
                    <>
                      <div className="space-y-4">
                        {recentTransactions.map((transaction) => (
                          <div key={transaction.id} className="flex items-center justify-between p-4 bg-muted/50 rounded-lg">
                            <div>
                              <p className="font-medium text-foreground">{transaction.description}</p>
                              <p className="text-sm text-muted-foreground">{formatDate(transaction.transactionDate)}</p>
                            </div>
                            <span className={`text-lg font-semibold ${transaction.amount >= 0 ? 'text-primary' : 'text-destructive'}`}>
                              {transaction.amount >= 0 ? '+' : '-'}{formatCurrency(transaction.amount)}
                            </span>
                          </div>
                        ))}
                      </div>
                      <button
                        onClick={() => setCurrentView('accounts')}
                        className="btn btn-outline w-full mt-4"
                      >
                        View All Transactions
                      </button>
                    </>
                  ) : (
                    <div className="text-center py-8">
                      <p className="text-muted-foreground">No recent transactions found</p>
                      <button
                        onClick={() => setCurrentView('accounts')}
                        className="btn btn-primary mt-4"
                      >
                        Add Your First Transaction
                      </button>
                    </div>
                  )}
                </div>
              </div>
            )}
          </>
        ) : currentView === 'accounts' ? (
          <AccountDashboard />
        ) : currentView === 'categories' ? (
          <CategoryManager />
        ) : null}
      </main>
    </div>
  );
};

export default Dashboard;