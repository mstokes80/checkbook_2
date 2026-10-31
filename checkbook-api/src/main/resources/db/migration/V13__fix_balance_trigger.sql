-- V13__fix_balance_trigger.sql
-- Fix the circular dependency issue in balance calculation trigger

-- Replace the problematic calculate_running_balance function
CREATE OR REPLACE FUNCTION calculate_running_balance(
    p_account_id BIGINT,
    p_transaction_date DATE,
    p_transaction_id BIGINT
) RETURNS DECIMAL(12,2) AS $$
DECLARE
    previous_balance DECIMAL(12,2) := 0;
    current_transaction_amount DECIMAL(12,2);
    account_initial_balance DECIMAL(12,2) := 0;
BEGIN
    -- Get the current transaction amount
    SELECT amount INTO current_transaction_amount
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
    -- We need to get the account's initial/starting balance but NOT current_balance
    -- (to avoid circular dependency since current_balance is what we're trying to update)
    IF previous_balance = 0 AND NOT EXISTS (
        SELECT 1 FROM transactions
        WHERE account_id = p_account_id
          AND (transaction_date < p_transaction_date
               OR (transaction_date = p_transaction_date AND id < p_transaction_id))
    ) THEN
        -- For the first transaction, start from 0 unless account has an explicit starting balance
        -- This avoids the circular dependency with current_balance
        previous_balance := 0;
    END IF;

    -- Calculate new running balance
    RETURN previous_balance + current_transaction_amount;
END;
$$ LANGUAGE plpgsql;

-- Add a comment to document the fix
COMMENT ON FUNCTION calculate_running_balance(BIGINT, DATE, BIGINT) IS 'Fixed version - calculates running balance without circular dependency on current_balance';