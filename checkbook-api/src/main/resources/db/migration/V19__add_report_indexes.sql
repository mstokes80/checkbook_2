-- V19: Add composite indexes for monthly spending reports performance optimization
-- These indexes optimize report queries for date ranges, category aggregation, and account filtering

-- Composite index for date range queries on transactions by account
-- Supports: WHERE account_id = ? AND transaction_date BETWEEN ? AND ?
-- Also supports: ORDER BY transaction_date DESC
CREATE INDEX IF NOT EXISTS idx_transactions_account_date
ON transactions(account_id, transaction_date DESC);

-- Composite index for category aggregation queries
-- Supports: GROUP BY category_id with date filtering
CREATE INDEX IF NOT EXISTS idx_transactions_category_date
ON transactions(category_id, transaction_date DESC);

-- Covering index for report queries (includes commonly queried columns)
-- Reduces table lookups by including amount and description in the index
-- Supports complex report queries filtering by account, category, and date
CREATE INDEX IF NOT EXISTS idx_transactions_report_query
ON transactions(account_id, category_id, transaction_date DESC)
INCLUDE (amount, description);