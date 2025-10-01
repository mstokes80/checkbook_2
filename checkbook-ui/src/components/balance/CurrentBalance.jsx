import React from 'react';
import { Clock } from 'lucide-react';
import { formatCurrency } from '../../utils/currency';
import { getBalanceColorClass, getBalanceAriaLabel } from '../../utils/balance';

const CurrentBalance = ({
  balance,
  lastUpdated,
  accountName,
  isLoading = false,
  showTimestamp = true,
  className = ''
}) => {
  const balanceColorClass = getBalanceColorClass(balance);
  const ariaLabel = getBalanceAriaLabel(balance, 'Current account balance');

  const formatLastUpdated = (timestamp) => {
    if (!timestamp) return null;

    try {
      const date = new Date(timestamp);
      return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        hour12: true
      });
    } catch (error) {
      return null;
    }
  };

  const formattedBalance = formatCurrency(balance);
  const formattedTimestamp = formatLastUpdated(lastUpdated);

  if (isLoading) {
    return (
      <div className={`current-balance-container ${className}`}>
        <div className="current-balance-header">
          <h2>Current Balance</h2>
          {accountName && <span className="account-name">{accountName}</span>}
        </div>
        <div className="current-balance text-muted-foreground">
          Loading...
        </div>
        {showTimestamp && (
          <div className="balance-updated">
            <Clock className="inline w-3 h-3 mr-1" />
            Updating...
          </div>
        )}
      </div>
    );
  }

  return (
    <div className={`current-balance-container ${className}`}>
      <div className="current-balance-header">
        <h2>Current Balance</h2>
        {accountName && <span className="account-name">{accountName}</span>}
      </div>

      <div
        className={`current-balance current-balance--${balanceColorClass.replace('balance-', '')}`}
        aria-label={ariaLabel}
        role="text"
      >
        {formattedBalance}
        <span className="sr-only">
          {balance >= 0 ? ' (positive balance)' : ' (negative balance)'}
        </span>
      </div>

      {showTimestamp && formattedTimestamp && (
        <div className="balance-updated">
          <Clock className="inline w-3 h-3 mr-1" />
          Last updated: {formattedTimestamp}
        </div>
      )}
    </div>
  );
};

export default CurrentBalance;