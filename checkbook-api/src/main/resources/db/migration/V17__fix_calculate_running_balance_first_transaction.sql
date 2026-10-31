-- V17__fix_calculate_running_balance_first_transaction.sql
-- Fix the calculate_running_balance function to properly handle the first transaction case

-- Replace the problematic calculate_running_balance function
CREATE OR REPLACE FUNCTION calculate_running_balance(
    p_account_id BIGINT,
    p_transaction_date DATE,
    p_transaction_id BIGINT
) RETURNS DECIMAL(12,2) AS $$
DECLARE
    previous_balance DECIMAL(12,2) := 0;
    current_transaction_amount DECIMAL(12,2) := 0;
    account_starting_balance DECIMAL(12,2) := 0;
    has_previous_transactions BOOLEAN := FALSE;
BEGIN
    -- Get the current transaction amount
    SELECT COALESCE(amount, 0) INTO current_transaction_amount
    FROM transactions
    WHERE id = p_transaction_id;

    -- Check if there are any previous transactions
    SELECT EXISTS (
        SELECT 1 FROM transactions
        WHERE account_id = p_account_id
          AND (transaction_date < p_transaction_date
               OR (transaction_date = p_transaction_date AND id < p_transaction_id))
    ) INTO has_previous_transactions;

    IF has_previous_transactions THEN
        -- Get the previous balance from the most recent transaction before this one
        SELECT COALESCE(running_balance, 0) INTO previous_balance
        FROM transactions
        WHERE account_id = p_account_id
          AND (transaction_date < p_transaction_date
               OR (transaction_date = p_transaction_date AND id < p_transaction_id))
        ORDER BY transaction_date DESC, id DESC
        LIMIT 1;
    ELSE
        -- This is the first transaction for the account
        -- The account's current_balance should be equal to this transaction amount
        SELECT COALESCE(current_balance, 0) INTO previous_balance
        FROM accounts
        WHERE id = p_account_id;
    END IF;

    -- Calculate new running balance
    RETURN previous_balance + current_transaction_amount;
END;
$$ LANGUAGE plpgsql;

-- Add a comment to document the fix
COMMENT ON FUNCTION calculate_running_balance(BIGINT, DATE, BIGINT) IS 'V17 - Fixed version that properly handles first transaction where account balance equals transaction amount';