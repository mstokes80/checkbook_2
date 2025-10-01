import React, { useState, useEffect } from 'react';
import { Plus, CreditCard, TrendingUp, TrendingDown, Users, Settings, Eye, EyeOff, History, ArrowUp, Shield, Receipt, DollarSign, X, BarChart3 } from 'lucide-react';
import { accountService } from '../../services/accountService';
import { transactionService } from '../../services/transactionService';
import { usePermissions, useAccountPermissionContext } from '../../hooks/usePermissions';
import CreateAccountForm from './CreateAccountForm';
import UpdateAccountForm from './UpdateAccountForm';
import PermissionManagement from './PermissionManagement';
import PermissionBadge from './PermissionBadge';
import PermissionGuard, { OwnerOnlyGuard, ViewPermissionGuard, TransactionPermissionGuard, FullAccessGuard } from './PermissionGuard';
import AuditLogViewer from './AuditLogViewer';
import PermissionRequestDialog from './PermissionRequestDialog';
import TransactionList from '../transactions/TransactionList';
import TransactionForm from '../transactions/TransactionForm';
import BulkCategorizeForm from '../transactions/BulkCategorizeForm';
import TransactionSummary from '../transactions/TransactionSummary';
import CurrentBalance from '../balance/CurrentBalance';

const AccountDashboard = () => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [showUpdateForm, setShowUpdateForm] = useState(null);
  const [showPermissions, setShowPermissions] = useState(null);
  const [showAuditLog, setShowAuditLog] = useState(null);
  const [showPermissionRequest, setShowPermissionRequest] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('all');

  // Transaction-related state
  const [showTransactions, setShowTransactions] = useState(null);
  const [showTransactionForm, setShowTransactionForm] = useState(null);
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [showBulkCategorize, setShowBulkCategorize] = useState(null);
  const [showTransactionSummary, setShowTransactionSummary] = useState(null);

  // Get permission context for all accounts
  const permissionContext = useAccountPermissionContext(accounts);

  useEffect(() => {
    loadAccounts();
  }, []);

  const loadAccounts = async () => {
    try {
      setLoading(true);
      const response = await accountService.getAccounts();
      if (response.success) {
        setAccounts(response.data || []);
      } else {
        setError(response.message || 'Failed to load accounts');
      }
    } catch (err) {
      setError('Failed to load accounts');
      console.error('Error loading accounts:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateAccount = async (accountData) => {
    try {
      const response = await accountService.createAccount(accountData);
      if (response.success) {
        setAccounts(prev => [...prev, response.data]);
        setShowCreateForm(false);
        return { success: true };
      } else {
        return { success: false, message: response.message };
      }
    } catch (err) {
      console.error('Error creating account:', err);
      return { success: false, message: 'Failed to create account' };
    }
  };

  const handleUpdateAccount = async (accountData) => {
    try {
      const response = await accountService.updateAccount(showUpdateForm.id, accountData);
      if (response.success) {
        setAccounts(prev => prev.map(account =>
          account.id === showUpdateForm.id ? { ...account, ...response.data } : account
        ));
        setShowUpdateForm(null);
        return { success: true };
      } else {
        return { success: false, message: response.message };
      }
    } catch (err) {
      console.error('Error updating account:', err);
      return { success: false, message: 'Failed to update account' };
    }
  };

  const handleDeleteAccount = async (accountId) => {
    if (!window.confirm('Are you sure you want to delete this account?')) return;

    try {
      const response = await accountService.deleteAccount(accountId);
      if (response.success) {
        setAccounts(prev => prev.filter(account => account.id !== accountId));
      } else {
        setError(response.message || 'Failed to delete account');
      }
    } catch (err) {
      setError('Failed to delete account');
      console.error('Error deleting account:', err);
    }
  };

  const getAccountTypeIcon = (type) => {
    switch (type) {
      case 'CHECKING':
        return <CreditCard className="h-5 w-5" />;
      case 'SAVINGS':
        return <TrendingUp className="h-5 w-5" />;
      case 'CREDIT_CARD':
        return <CreditCard className="h-5 w-5 text-destructive" />;
      case 'INVESTMENT':
        return <TrendingUp className="h-5 w-5 text-primary" />;
      default:
        return <CreditCard className="h-5 w-5" />;
    }
  };

  const getAccountTypeColor = (type) => {
    switch (type) {
      case 'CHECKING':
        return 'text-blue-600';
      case 'SAVINGS':
        return 'text-green-600';
      case 'CREDIT_CARD':
        return 'text-red-600';
      case 'INVESTMENT':
        return 'text-purple-600';
      default:
        return 'text-gray-600';
    }
  };

  const formatBalance = (balance) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(balance);
  };

  const getTotalBalance = () => {
    return accounts.reduce((total, account) => {
      return account.isOwner ? total + parseFloat(account.currentBalance || 0) : total;
    }, 0);
  };

  const getFilteredAccounts = () => {
    let filtered = accounts;

    if (searchTerm) {
      filtered = filtered.filter(account =>
        account.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        account.bankName?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    if (filterType !== 'all') {
      if (filterType === 'owned') {
        filtered = filtered.filter(account => account.isOwner);
      } else if (filterType === 'shared') {
        filtered = filtered.filter(account => !account.isOwner);
      } else {
        filtered = filtered.filter(account => account.accountType === filterType);
      }
    }

    return filtered;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
          <p className="mt-2 text-muted-foreground">Loading accounts...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Accounts</h1>
          <p className="text-muted-foreground">Manage your financial accounts</p>
        </div>
        <button
          onClick={() => setShowCreateForm(true)}
          className="btn btn-primary"
        >
          <Plus className="h-4 w-4 mr-2" />
          Add Account
        </button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="card p-4 border-destructive bg-destructive/10">
          <p className="text-destructive text-sm">{error}</p>
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Total Balance</p>
              <p className="text-2xl font-bold text-foreground">
                {formatBalance(getTotalBalance())}
              </p>
            </div>
            <TrendingUp className="h-8 w-8 text-primary" />
          </div>
        </div>

        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Total Accounts</p>
              <p className="text-2xl font-bold text-foreground">{accounts.length}</p>
            </div>
            <CreditCard className="h-8 w-8 text-primary" />
          </div>
        </div>

        <div className="card p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-muted-foreground">Shared Access</p>
              <p className="text-2xl font-bold text-foreground">
                {permissionContext.sharedAccounts.length}
              </p>
              <p className="text-xs text-muted-foreground mt-1">
                {permissionContext.ownedAccounts.length} owned
              </p>
            </div>
            <Users className="h-8 w-8 text-primary" />
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="card p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Search accounts..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input w-full"
            />
          </div>
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="input w-full sm:w-48"
          >
            <option value="all">All Accounts</option>
            <option value="owned">My Accounts</option>
            <option value="shared">Shared with Me</option>
            <option value="CHECKING">Checking</option>
            <option value="SAVINGS">Savings</option>
            <option value="CREDIT_CARD">Credit Card</option>
            <option value="INVESTMENT">Investment</option>
            <option value="CASH">Cash</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
      </div>

      {/* Accounts Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {getFilteredAccounts().map((account) => (
          <div key={account.id} className="card p-6 hover:shadow-lg transition-shadow">
            <div className="flex items-start justify-between mb-4">
              <div className="flex items-center space-x-3">
                <div className={`p-2 rounded-lg bg-muted ${getAccountTypeColor(account.accountType)}`}>
                  {getAccountTypeIcon(account.accountType)}
                </div>
                <div>
                  <h3 className="font-semibold text-foreground">{account.name}</h3>
                  <p className="text-sm text-muted-foreground">
                    {account.accountType.replace('_', ' ')}
                  </p>
                </div>
              </div>
              <div className="flex items-center space-x-2">
                {account.isShared && (
                  <Users className="h-4 w-4 text-muted-foreground" />
                )}
                {!account.isOwner && account.userPermission && (
                  <PermissionBadge
                    permissionType={account.userPermission}
                    size="small"
                  />
                )}
                {account.isOwner && (
                  <div className="text-xs bg-green-100 text-green-800 px-2 py-1 rounded font-medium">
                    Owner
                  </div>
                )}
              </div>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">Balance</span>
                <span className="font-semibold text-foreground">
                  {formatBalance(account.currentBalance)}
                </span>
              </div>

              {account.bankName && (
                <div className="flex justify-between items-center">
                  <span className="text-sm text-muted-foreground">Bank</span>
                  <span className="text-sm text-foreground">{account.bankName}</span>
                </div>
              )}

              {account.accountNumberMasked && (
                <div className="flex justify-between items-center">
                  <span className="text-sm text-muted-foreground">Account</span>
                  <span className="text-sm text-foreground font-mono">
                    {account.accountNumberMasked}
                  </span>
                </div>
              )}

              {!account.isOwner && (
                <div className="flex justify-between items-center">
                  <span className="text-sm text-muted-foreground">Owner</span>
                  <span className="text-sm text-foreground">{account.ownerName}</span>
                </div>
              )}
            </div>

            {/* Actions */}
            <ViewPermissionGuard account={account}>
              <div className="mt-4 pt-4 border-t border-border">
                <div className="flex justify-between items-start">
                  <div className="flex flex-wrap gap-2">
                    {/* View Transactions - All users with access */}
                    <ViewPermissionGuard account={account}>
                      <button
                        onClick={() => setShowTransactions(account.id)}
                        className="btn btn-outline btn-sm"
                        title="View Transactions"
                      >
                        <Receipt className="h-4 w-4" />
                      </button>
                    </ViewPermissionGuard>

                    {/* Add Transaction - Transaction permission required */}
                    <TransactionPermissionGuard account={account}>
                      <button
                        onClick={() => setShowTransactionForm(account.id)}
                        className="btn btn-outline btn-sm"
                        title="Add Transaction"
                      >
                        <DollarSign className="h-4 w-4" />
                      </button>
                    </TransactionPermissionGuard>

                    {/* Transaction Summary - All users with access */}
                    <ViewPermissionGuard account={account}>
                      <button
                        onClick={() => setShowTransactionSummary(account.id)}
                        className="btn btn-outline btn-sm"
                        title="Transaction Summary"
                      >
                        <BarChart3 className="h-4 w-4" />
                      </button>
                    </ViewPermissionGuard>

                    {/* View Activity - All users with access */}
                    <ViewPermissionGuard account={account}>
                      <button
                        onClick={() => setShowAuditLog(account.id)}
                        className="btn btn-outline btn-sm"
                        title="View Activity"
                      >
                        <History className="h-4 w-4" />
                      </button>
                    </ViewPermissionGuard>

                    {/* Permission Management - Owner only */}
                    <OwnerOnlyGuard account={account}>
                      <button
                        onClick={() => setShowPermissions(account.id)}
                        className="btn btn-outline btn-sm"
                        title="Manage Permissions"
                      >
                        <Users className="h-4 w-4" />
                      </button>
                    </OwnerOnlyGuard>

                    {/* Account Settings - Full access required */}
                    <FullAccessGuard account={account}>
                      <button
                        onClick={() => setShowUpdateForm(account)}
                        className="btn btn-outline btn-sm"
                        title="Settings"
                      >
                        <Settings className="h-4 w-4" />
                      </button>
                    </FullAccessGuard>

                    {/* Permission Request - Non-owners with upgrade potential */}
                    {!account.isOwner && account.userPermission && (
                      <PermissionGuard
                        account={account}
                        fallback={
                          <button
                            onClick={() => setShowPermissionRequest({ account, currentPermission: account.userPermission })}
                            className="btn btn-outline btn-sm text-blue-600 hover:bg-blue-50"
                            title="Request Permission Upgrade"
                          >
                            <ArrowUp className="h-4 w-4" />
                          </button>
                        }
                      >
                        {null}
                      </PermissionGuard>
                    )}
                  </div>

                  {/* Delete Account - Owner only */}
                  <OwnerOnlyGuard account={account}>
                    <button
                      onClick={() => handleDeleteAccount(account.id)}
                      className="btn btn-outline btn-sm text-destructive hover:bg-destructive hover:text-destructive-foreground"
                      title="Delete Account"
                    >
                      Delete
                    </button>
                  </OwnerOnlyGuard>
                </div>
              </div>
            </ViewPermissionGuard>
          </div>
        ))}
      </div>

      {getFilteredAccounts().length === 0 && (
        <div className="text-center py-12">
          <CreditCard className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-foreground mb-2">No accounts found</h3>
          <p className="text-muted-foreground mb-4">
            {searchTerm || filterType !== 'all'
              ? 'Try adjusting your search or filter criteria.'
              : 'Get started by creating your first account.'}
          </p>
          {!searchTerm && filterType === 'all' && (
            <button
              onClick={() => setShowCreateForm(true)}
              className="btn btn-primary"
            >
              <Plus className="h-4 w-4 mr-2" />
              Create Account
            </button>
          )}
        </div>
      )}

      {/* Create Account Modal */}
      {showCreateForm && (
        <CreateAccountForm
          onClose={() => setShowCreateForm(false)}
          onSubmit={handleCreateAccount}
        />
      )}

      {/* Update Account Modal */}
      {showUpdateForm && (
        <UpdateAccountForm
          account={showUpdateForm}
          onClose={() => setShowUpdateForm(null)}
          onSubmit={handleUpdateAccount}
        />
      )}

      {/* Permission Management Modal */}
      {showPermissions && (
        <PermissionManagement
          accountId={showPermissions}
          onClose={() => setShowPermissions(null)}
          onUpdate={loadAccounts}
        />
      )}

      {/* Audit Log Viewer Modal */}
      {showAuditLog && (
        <AuditLogViewer
          accountId={showAuditLog}
          onClose={() => setShowAuditLog(null)}
          accountService={accountService}
        />
      )}

      {/* Permission Request Dialog */}
      {showPermissionRequest && (
        <PermissionRequestDialog
          account={showPermissionRequest.account}
          currentPermission={showPermissionRequest.currentPermission}
          onClose={() => setShowPermissionRequest(null)}
          onRequestSubmitted={() => {
            // You could add a notification here
            console.log('Permission request submitted');
          }}
          permissionRequestService={{
            createPermissionRequest: async (data) => {
              // This would call your permission request service
              // For now, return a mock response
              return { success: true, data: { id: Date.now(), ...data } };
            }
          }}
        />
      )}

      {/* Transaction List Modal */}
      {showTransactions && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-10 mx-auto p-5 border w-full max-w-7xl shadow-lg rounded-md bg-white">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-medium text-gray-900">
                Transactions - {accounts.find(acc => acc.id === showTransactions)?.name}
              </h3>
              <button
                onClick={() => setShowTransactions(null)}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <X className="h-6 w-6" />
              </button>
            </div>

            <CurrentBalance
              balance={accounts.find(acc => acc.id === showTransactions)?.currentBalance || 0}
              lastUpdated={new Date().toISOString()}
              accountName={accounts.find(acc => acc.id === showTransactions)?.name}
            />

            <TransactionList
              accountId={showTransactions}
              onTransactionEdit={(transaction) => {
                setEditingTransaction(transaction);
                setShowTransactionForm(showTransactions);
              }}
              onTransactionDelete={async (transaction) => {
                if (window.confirm('Are you sure you want to delete this transaction?')) {
                  try {
                    const response = await transactionService.deleteTransaction(transaction.id);
                    if (response.success) {
                      // Refresh the transaction list by re-triggering the load
                      setShowTransactions(null);
                      setTimeout(() => setShowTransactions(showTransactions), 100);
                    }
                  } catch (err) {
                    console.error('Error deleting transaction:', err);
                  }
                }
              }}
            />
          </div>
        </div>
      )}

      {/* Transaction Form Modal */}
      {showTransactionForm && (
        <TransactionForm
          transaction={editingTransaction}
          accountId={showTransactionForm}
          onClose={() => {
            setShowTransactionForm(null);
            setEditingTransaction(null);
          }}
          onSubmit={(response) => {
            // Refresh transactions if they're currently being viewed
            if (showTransactions === showTransactionForm) {
              setShowTransactions(null);
              setTimeout(() => setShowTransactions(showTransactionForm), 100);
            }
            // Refresh account data to update balances
            loadAccounts();
          }}
          mode={editingTransaction ? 'edit' : 'create'}
        />
      )}

      {/* Bulk Categorize Modal */}
      {showBulkCategorize && (
        <BulkCategorizeForm
          transactions={showBulkCategorize.transactions || []}
          onClose={() => setShowBulkCategorize(null)}
          onComplete={(response) => {
            // Refresh transactions if they're currently being viewed
            if (showTransactions) {
              setShowTransactions(null);
              setTimeout(() => setShowTransactions(showBulkCategorize.accountId), 100);
            }
          }}
        />
      )}

      {/* Transaction Summary Modal */}
      {showTransactionSummary && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-10 mx-auto p-5 border w-full max-w-6xl shadow-lg rounded-md bg-white">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-medium text-gray-900">
                Transaction Summary - {accounts.find(acc => acc.id === showTransactionSummary)?.name}
              </h3>
              <button
                onClick={() => setShowTransactionSummary(null)}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <X className="h-6 w-6" />
              </button>
            </div>
            <TransactionSummary
              accountId={showTransactionSummary}
              onClose={() => setShowTransactionSummary(null)}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default AccountDashboard;