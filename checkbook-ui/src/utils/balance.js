/**
 * Balance utilities for classification, analysis, and display logic
 * Used for running balance display functionality
 */

/**
 * Gets the appropriate CSS class for a balance amount
 * @param {number|string} balance - The balance amount
 * @returns {string} CSS class name for styling
 */
export const getBalanceColorClass = (balance) => {
  const numericBalance = typeof balance === 'string' ? parseFloat(balance) : Number(balance);

  if (isNaN(numericBalance)) {
    return 'balance-zero';
  }

  if (numericBalance > 0) {
    return 'balance-positive';
  } else if (numericBalance < 0) {
    return 'balance-negative';
  } else {
    return 'balance-zero';
  }
};

/**
 * Determines if a balance change is significant enough to highlight
 * @param {number|string} previousBalance - Previous balance amount
 * @param {number|string} currentBalance - Current balance amount
 * @param {number} threshold - Threshold for significant change (default: 100)
 * @returns {boolean} True if change is significant
 */
export const isSignificantBalanceChange = (previousBalance, currentBalance, threshold = 100) => {
  const prevBalance = typeof previousBalance === 'string' ? parseFloat(previousBalance) : Number(previousBalance);
  const currBalance = typeof currentBalance === 'string' ? parseFloat(currentBalance) : Number(currentBalance);

  if (isNaN(prevBalance) || isNaN(currBalance)) {
    return false;
  }

  const change = Math.abs(currBalance - prevBalance);
  return change >= threshold;
};

/**
 * Gets the balance change direction and type
 * @param {number|string} previousBalance - Previous balance amount
 * @param {number|string} currentBalance - Current balance amount
 * @returns {Object} Change information with direction, amount, and type
 */
export const getBalanceChange = (previousBalance, currentBalance) => {
  const prevBalance = typeof previousBalance === 'string' ? parseFloat(previousBalance) : Number(previousBalance);
  const currBalance = typeof currentBalance === 'string' ? parseFloat(currentBalance) : Number(currentBalance);

  if (isNaN(prevBalance) || isNaN(currBalance)) {
    return {
      direction: 'none',
      amount: 0,
      percentage: 0,
      type: 'invalid'
    };
  }

  const change = currBalance - prevBalance;
  const percentage = prevBalance !== 0 ? (Math.abs(change) / Math.abs(prevBalance)) * 100 : 0;

  return {
    direction: change > 0 ? 'increase' : change < 0 ? 'decrease' : 'none',
    amount: Math.abs(change),
    percentage: percentage,
    type: Math.abs(change) >= 100 ? 'significant' : 'minor'
  };
};

/**
 * Determines the balance status category
 * @param {number|string} balance - The balance amount
 * @param {Object} thresholds - Custom thresholds for categorization
 * @returns {string} Balance status category
 */
export const getBalanceStatus = (balance, thresholds = {}) => {
  const {
    criticalLow = -1000,
    low = 0,
    healthy = 1000
  } = thresholds;

  const numericBalance = typeof balance === 'string' ? parseFloat(balance) : Number(balance);

  if (isNaN(numericBalance)) {
    return 'unknown';
  }

  if (numericBalance <= criticalLow) {
    return 'critical-low';
  } else if (numericBalance <= low) {
    return 'low';
  } else if (numericBalance < healthy) {
    return 'moderate';
  } else {
    return 'healthy';
  }
};

/**
 * Gets an indicator symbol for balance change direction
 * @param {number|string} previousBalance - Previous balance amount
 * @param {number|string} currentBalance - Current balance amount
 * @param {number} threshold - Threshold for showing indicator (default: 50)
 * @returns {string} Indicator symbol or empty string
 */
export const getBalanceChangeIndicator = (previousBalance, currentBalance, threshold = 50) => {
  const change = getBalanceChange(previousBalance, currentBalance);

  if (change.type === 'invalid' || change.amount < threshold) {
    return '';
  }

  switch (change.direction) {
    case 'increase':
      return '↗';
    case 'decrease':
      return '↘';
    default:
      return '';
  }
};

/**
 * Formats balance change as a readable string
 * @param {number|string} previousBalance - Previous balance amount
 * @param {number|string} currentBalance - Current balance amount
 * @param {Object} options - Formatting options
 * @returns {string} Formatted change description
 */
export const formatBalanceChange = (previousBalance, currentBalance, options = {}) => {
  const { showPercentage = false, currency = true } = options;
  const change = getBalanceChange(previousBalance, currentBalance);

  if (change.type === 'invalid' || change.direction === 'none') {
    return '';
  }

  const direction = change.direction === 'increase' ? 'increased' : 'decreased';
  const amount = currency ? `$${change.amount.toFixed(2)}` : change.amount.toFixed(2);

  let result = `Balance ${direction} by ${amount}`;

  if (showPercentage && change.percentage > 0) {
    result += ` (${change.percentage.toFixed(1)}%)`;
  }

  return result;
};

/**
 * Checks if a balance indicates overdraft
 * @param {number|string} balance - The balance amount
 * @param {number} overdraftLimit - Overdraft limit (default: 0)
 * @returns {boolean} True if account is in overdraft
 */
export const isOverdraft = (balance, overdraftLimit = 0) => {
  const numericBalance = typeof balance === 'string' ? parseFloat(balance) : Number(balance);
  return !isNaN(numericBalance) && numericBalance < -Math.abs(overdraftLimit);
};

/**
 * Gets the appropriate ARIA label for a balance amount
 * @param {number|string} balance - The balance amount
 * @param {string} context - Context for the label (default: 'Balance')
 * @returns {string} ARIA label for accessibility
 */
export const getBalanceAriaLabel = (balance, context = 'Balance') => {
  const numericBalance = typeof balance === 'string' ? parseFloat(balance) : Number(balance);

  if (isNaN(numericBalance)) {
    return `${context}: Unknown amount`;
  }

  const formattedAmount = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD'
  }).format(Math.abs(numericBalance));

  if (numericBalance >= 0) {
    return `${context}: ${formattedAmount} positive balance`;
  } else {
    return `${context}: ${formattedAmount} negative balance`;
  }
};

/**
 * Calculates running balances for a list of transactions
 * @param {Array} transactions - Array of transaction objects
 * @param {number} startingBalance - Starting balance (default: 0)
 * @returns {Array} Transactions with calculated running balances
 */
export const calculateRunningBalances = (transactions, startingBalance = 0) => {
  if (!Array.isArray(transactions)) {
    return [];
  }

  let currentBalance = startingBalance;

  return transactions.map(transaction => {
    currentBalance += (transaction.amount || 0);
    return {
      ...transaction,
      runningBalance: currentBalance
    };
  });
};