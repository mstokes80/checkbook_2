import React, { useState, useEffect } from 'react';
import {
  X,
  History,
  Filter,
  RefreshCw,
  ChevronDown,
  ChevronUp,
  Shield,
  ShieldCheck,
  ShieldX,
  Eye,
  Edit,
  CreditCard,
  User,
  Calendar
} from 'lucide-react';
import PermissionBadge from './PermissionBadge';

const AuditLogViewer = ({ accountId, onClose, accountService }) => {
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pagination, setPagination] = useState({
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0
  });

  // Filters
  const [filters, setFilters] = useState({
    actionType: '',
    userId: '',
    startDate: '',
    endDate: ''
  });
  const [showFilters, setShowFilters] = useState(false);
  const [expandedEntries, setExpandedEntries] = useState(new Set());

  useEffect(() => {
    loadAuditLogs();
  }, [accountId, pagination.page, pagination.size]);

  const loadAuditLogs = async () => {
    try {
      setLoading(true);
      setError('');

      const queryParams = {
        page: pagination.page,
        size: pagination.size,
        ...Object.fromEntries(
          Object.entries(filters).filter(([_, value]) => value && value.trim() !== '')
        )
      };

      const response = await accountService.getAccountAuditLogs(accountId, queryParams);

      if (response.success) {
        const data = response.data;
        setAuditLogs(data.content || []);
        setPagination(prev => ({
          ...prev,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0
        }));
      } else {
        setError(response.message || 'Failed to load audit logs');
      }
    } catch (err) {
      setError('Failed to load audit logs');
      console.error('Error loading audit logs:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleFilterChange = (filterName, value) => {
    setFilters(prev => ({
      ...prev,
      [filterName]: value
    }));
  };

  const applyFilters = () => {
    setPagination(prev => ({ ...prev, page: 0 }));
    loadAuditLogs();
  };

  const clearFilters = () => {
    setFilters({
      actionType: '',
      userId: '',
      startDate: '',
      endDate: ''
    });
    setPagination(prev => ({ ...prev, page: 0 }));
    setTimeout(loadAuditLogs, 0);
  };

  const toggleExpanded = (logId) => {
    setExpandedEntries(prev => {
      const newSet = new Set(prev);
      if (newSet.has(logId)) {
        newSet.delete(logId);
      } else {
        newSet.add(logId);
      }
      return newSet;
    });
  };

  const getActionIcon = (actionType) => {
    switch (actionType) {
      case 'PERMISSION_GRANTED':
        return <ShieldCheck className="h-4 w-4 text-green-600" />;
      case 'PERMISSION_MODIFIED':
        return <Shield className="h-4 w-4 text-blue-600" />;
      case 'PERMISSION_REVOKED':
        return <ShieldX className="h-4 w-4 text-red-600" />;
      case 'ACCOUNT_VIEWED':
        return <Eye className="h-4 w-4 text-blue-600" />;
      case 'TRANSACTION_ADDED':
        return <CreditCard className="h-4 w-4 text-green-600" />;
      case 'ACCOUNT_MODIFIED':
        return <Edit className="h-4 w-4 text-orange-600" />;
      case 'PERMISSION_REQUESTED':
        return <Shield className="h-4 w-4 text-purple-600" />;
      case 'PERMISSION_REQUEST_APPROVED':
        return <ShieldCheck className="h-4 w-4 text-green-600" />;
      case 'PERMISSION_REQUEST_DENIED':
        return <ShieldX className="h-4 w-4 text-red-600" />;
      default:
        return <History className="h-4 w-4 text-gray-600" />;
    }
  };

  const getActionDescription = (log) => {
    const details = log.actionDetails || {};

    switch (log.actionType) {
      case 'PERMISSION_GRANTED':
        return `Granted ${details.permissionType || 'permission'} to ${details.targetUsername || 'user'}`;
      case 'PERMISSION_MODIFIED':
        return `Modified permission from ${details.oldPermissionType || 'previous'} to ${details.newPermissionType || 'new'} for ${details.targetUsername || 'user'}`;
      case 'PERMISSION_REVOKED':
        return `Revoked ${details.permissionType || 'permission'} from ${details.targetUsername || 'user'}`;
      case 'ACCOUNT_VIEWED':
        return `Account viewed by ${details.viewingUsername || 'user'}`;
      case 'TRANSACTION_ADDED':
        return `Transaction added: ${details.description || 'transaction'}`;
      case 'ACCOUNT_MODIFIED':
        return `Account details modified`;
      case 'PERMISSION_REQUESTED':
        return `Permission escalation requested: ${details.requestedPermission || 'permission'}`;
      case 'PERMISSION_REQUEST_APPROVED':
        return `Permission request approved for ${details.requestingUsername || 'user'}`;
      case 'PERMISSION_REQUEST_DENIED':
        return `Permission request denied for ${details.requestingUsername || 'user'}`;
      default:
        return log.actionType.replace(/_/g, ' ').toLowerCase();
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const actionTypes = [
    'PERMISSION_GRANTED',
    'PERMISSION_MODIFIED',
    'PERMISSION_REVOKED',
    'ACCOUNT_VIEWED',
    'TRANSACTION_ADDED',
    'ACCOUNT_MODIFIED',
    'PERMISSION_REQUESTED',
    'PERMISSION_REQUEST_APPROVED',
    'PERMISSION_REQUEST_DENIED'
  ];

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="card max-w-6xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-6 border-b border-border flex-shrink-0">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-2xl font-bold text-foreground">Account Activity Log</h2>
              <p className="text-muted-foreground">View all activity and changes for this account</p>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-muted rounded-lg transition-colors"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Actions Row */}
          <div className="flex items-center space-x-3">
            <button
              onClick={() => setShowFilters(!showFilters)}
              className="btn btn-outline"
            >
              <Filter className="h-4 w-4 mr-2" />
              {showFilters ? 'Hide Filters' : 'Show Filters'}
            </button>
            <button
              onClick={loadAuditLogs}
              className="btn btn-outline"
              disabled={loading}
            >
              <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
            <div className="text-sm text-muted-foreground">
              {pagination.totalElements} entries
            </div>
          </div>

          {/* Filters */}
          {showFilters && (
            <div className="mt-4 p-4 bg-muted/30 rounded-lg">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div>
                  <label className="label mb-2 block">Action Type</label>
                  <select
                    value={filters.actionType}
                    onChange={(e) => handleFilterChange('actionType', e.target.value)}
                    className="input w-full"
                  >
                    <option value="">All Actions</option>
                    {actionTypes.map(type => (
                      <option key={type} value={type}>
                        {type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="label mb-2 block">Start Date</label>
                  <input
                    type="datetime-local"
                    value={filters.startDate}
                    onChange={(e) => handleFilterChange('startDate', e.target.value)}
                    className="input w-full"
                  />
                </div>
                <div>
                  <label className="label mb-2 block">End Date</label>
                  <input
                    type="datetime-local"
                    value={filters.endDate}
                    onChange={(e) => handleFilterChange('endDate', e.target.value)}
                    className="input w-full"
                  />
                </div>
                <div className="flex items-end space-x-2">
                  <button onClick={applyFilters} className="btn btn-primary">
                    Apply
                  </button>
                  <button onClick={clearFilters} className="btn btn-outline">
                    Clear
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {error && (
            <div className="mb-6 p-4 bg-destructive/10 border border-destructive/20 rounded-lg">
              <p className="text-destructive text-sm">{error}</p>
            </div>
          )}

          {loading ? (
            <div className="text-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto"></div>
              <p className="mt-2 text-muted-foreground">Loading audit logs...</p>
            </div>
          ) : auditLogs.length === 0 ? (
            <div className="text-center py-12">
              <History className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
              <h4 className="text-lg font-semibold text-foreground mb-2">No Activity Found</h4>
              <p className="text-muted-foreground">
                {Object.values(filters).some(f => f)
                  ? 'No activity matches your current filters.'
                  : 'No activity has been recorded for this account yet.'}
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {auditLogs.map((log) => (
                <div key={log.id} className="card p-4 hover:shadow-md transition-shadow">
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-3 flex-1">
                      <div className="flex-shrink-0 p-2 bg-muted rounded-lg">
                        {getActionIcon(log.actionType)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center space-x-2 mb-1">
                          <p className="font-medium text-foreground">
                            {getActionDescription(log)}
                          </p>
                          {log.actionDetails?.permissionType && (
                            <PermissionBadge
                              permissionType={log.actionDetails.permissionType}
                              size="small"
                            />
                          )}
                        </div>
                        <div className="flex items-center space-x-4 text-sm text-muted-foreground">
                          <div className="flex items-center space-x-1">
                            <Calendar className="h-3 w-3" />
                            <span>{formatDate(log.createdAt)}</span>
                          </div>
                          {log.ipAddress && (
                            <span>IP: {log.ipAddress}</span>
                          )}
                        </div>
                      </div>
                    </div>

                    {log.actionDetails && Object.keys(log.actionDetails).length > 0 && (
                      <button
                        onClick={() => toggleExpanded(log.id)}
                        className="p-1 hover:bg-muted rounded transition-colors"
                      >
                        {expandedEntries.has(log.id) ? (
                          <ChevronUp className="h-4 w-4" />
                        ) : (
                          <ChevronDown className="h-4 w-4" />
                        )}
                      </button>
                    )}
                  </div>

                  {/* Expanded Details */}
                  {expandedEntries.has(log.id) && log.actionDetails && (
                    <div className="mt-3 pt-3 border-t border-border">
                      <h5 className="font-medium text-foreground mb-2">Details</h5>
                      <div className="bg-muted/30 p-3 rounded text-sm">
                        <pre className="whitespace-pre-wrap text-muted-foreground">
                          {JSON.stringify(log.actionDetails, null, 2)}
                        </pre>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pagination */}
        {!loading && auditLogs.length > 0 && (
          <div className="p-6 border-t border-border flex-shrink-0">
            <div className="flex items-center justify-between">
              <div className="text-sm text-muted-foreground">
                Showing {pagination.page * pagination.size + 1} to{' '}
                {Math.min((pagination.page + 1) * pagination.size, pagination.totalElements)} of{' '}
                {pagination.totalElements} entries
              </div>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => setPagination(prev => ({ ...prev, page: prev.page - 1 }))}
                  disabled={pagination.page === 0}
                  className="btn btn-outline"
                >
                  Previous
                </button>
                <span className="text-sm text-muted-foreground">
                  Page {pagination.page + 1} of {pagination.totalPages}
                </span>
                <button
                  onClick={() => setPagination(prev => ({ ...prev, page: prev.page + 1 }))}
                  disabled={pagination.page >= pagination.totalPages - 1}
                  className="btn btn-outline"
                >
                  Next
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AuditLogViewer;