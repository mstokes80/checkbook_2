-- V16__fix_null_running_balance.sql
-- Fix the calculate_running_balance function to handle NULL values properly

-- Replace the problematic calculate_running_balance function
CREATE OR REPLACE FUNCTION calculate_running_balance(
    p_account_id BIGINT,
    p_transaction_date DATE,
    p_transaction_id BIGINT
) RETURNS DECIMAL(12,2) AS $$
DECLARE
    previous_balance DECIMAL(12,2) := 0;
    current_transaction_amount DECIMAL(12,2) := 0;
BEGIN
    -- Get the current transaction amount
    SELECT COALESCE(amount, 0) INTO current_transaction_amount
    FROM transactions
    WHERE id = p_transaction_id;

    -- Get the previous balance from the most recent transaction before this one
    SELECT COALESCE(running_balance, 0) INTO previous_balance
    FROM transactions
    WHERE account_id = p_account_id
      AND (transaction_date < p_transaction_date
           OR (transaction_date = p_transaction_date AND id < p_transaction_id))
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1;

    -- If no previous transaction found, this is the first transaction for the account
    -- Start from the account's current balance (which represents the starting balance)
    IF previous_balance = 0 AND NOT EXISTS (
        SELECT 1 FROM transactions
        WHERE account_id = p_account_id
          AND (transaction_date < p_transaction_date
               OR (transaction_date = p_transaction_date AND id < p_transaction_id))
    ) THEN
        -- For the first transaction, get the account's initial balance
        SELECT COALESCE(current_balance, 0) INTO previous_balance
        FROM accounts
        WHERE id = p_account_id;

        -- Subtract this transaction's amount since current_balance includes all transactions
        previous_balance := previous_balance - current_transaction_amount;
    END IF;

    -- Calculate new running balance
    RETURN previous_balance + current_transaction_amount;
END;
$$ LANGUAGE plpgsql;

-- Add a comment to document the fix
COMMENT ON FUNCTION calculate_running_balance(BIGINT, DATE, BIGINT) IS 'Fixed version - properly handles NULL values and calculates running balance correctly';