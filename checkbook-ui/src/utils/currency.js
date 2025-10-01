/**
 * Currency formatting utilities for the Checkbook application
 * Provides consistent currency display across all components
 */

/**
 * Formats a number as currency with consistent formatting
 * @param {number|string|null|undefined} amount - The amount to format
 * @param {Object} options - Formatting options
 * @param {string} options.currency - Currency code (default: 'USD')
 * @param {string} options.locale - Locale for formatting (default: 'en-US')
 * @param {number} options.minimumFractionDigits - Minimum decimal places (default: 2)
 * @param {number} options.maximumFractionDigits - Maximum decimal places (default: 2)
 * @returns {string} Formatted currency string
 */
export const formatCurrency = (amount, options = {}) => {
  const {
    currency = 'USD',
    locale = 'en-US',
    minimumFractionDigits = 2,
    maximumFractionDigits = 2
  } = options;

  // Handle null, undefined, or empty values
  if (amount === null || amount === undefined || amount === '') {
    return formatCurrency(0, options);
  }

  // Convert to number if string
  const numericAmount = typeof amount === 'string' ? parseFloat(amount) : Number(amount);

  // Handle invalid numbers
  if (isNaN(numericAmount)) {
    return formatCurrency(0, options);
  }

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits,
    maximumFractionDigits
  }).format(numericAmount);
};

/**
 * Formats currency without the currency symbol
 * @param {number|string|null|undefined} amount - The amount to format
 * @param {Object} options - Formatting options
 * @returns {string} Formatted number string without currency symbol
 */
export const formatCurrencyNumber = (amount, options = {}) => {
  const {
    locale = 'en-US',
    minimumFractionDigits = 2,
    maximumFractionDigits = 2
  } = options;

  // Handle null, undefined, or empty values
  if (amount === null || amount === undefined || amount === '') {
    return formatCurrencyNumber(0, options);
  }

  // Convert to number if string
  const numericAmount = typeof amount === 'string' ? parseFloat(amount) : Number(amount);

  // Handle invalid numbers
  if (isNaN(numericAmount)) {
    return formatCurrencyNumber(0, options);
  }

  return new Intl.NumberFormat(locale, {
    minimumFractionDigits,
    maximumFractionDigits
  }).format(numericAmount);
};

/**
 * Formats currency with explicit positive/negative indicators
 * @param {number|string|null|undefined} amount - The amount to format
 * @param {Object} options - Formatting options
 * @param {boolean} options.showPositiveSign - Show + for positive amounts
 * @returns {string} Formatted currency with sign indicators
 */
export const formatCurrencyWithSign = (amount, options = {}) => {
  const { showPositiveSign = false, ...formatOptions } = options;

  const numericAmount = typeof amount === 'string' ? parseFloat(amount) : Number(amount);

  if (isNaN(numericAmount)) {
    return formatCurrency(0, formatOptions);
  }

  const formatted = formatCurrency(numericAmount, formatOptions);

  if (showPositiveSign && numericAmount > 0) {
    return `+${formatted}`;
  }

  return formatted;
};

/**
 * Parses a currency string back to a number
 * @param {string} currencyString - Currency string to parse
 * @returns {number} Parsed number value
 */
export const parseCurrency = (currencyString) => {
  if (!currencyString || typeof currencyString !== 'string') {
    return 0;
  }

  // Remove currency symbols, commas, and extra spaces
  const cleanString = currencyString
    .replace(/[$,\s]/g, '')
    .replace(/[()]/g, '-'); // Convert parentheses to negative sign

  const parsed = parseFloat(cleanString);
  return isNaN(parsed) ? 0 : parsed;
};

/**
 * Checks if an amount is considered a "large" transaction
 * @param {number} amount - The amount to check
 * @param {number} threshold - The threshold for large amounts (default: 1000)
 * @returns {boolean} True if amount is large
 */
export const isLargeAmount = (amount, threshold = 1000) => {
  const numericAmount = typeof amount === 'string' ? parseFloat(amount) : Number(amount);
  return Math.abs(numericAmount) >= threshold;
};

/**
 * Gets the absolute value formatted as currency
 * @param {number|string} amount - The amount to format
 * @param {Object} options - Formatting options
 * @returns {string} Formatted absolute value as currency
 */
export const formatAbsoluteCurrency = (amount, options = {}) => {
  const numericAmount = typeof amount === 'string' ? parseFloat(amount) : Number(amount);
  return formatCurrency(Math.abs(numericAmount), options);
};