-- V15__fix_ambiguous_column_reference.sql
-- Fix ambiguous column reference in recalculate_subsequent_balances function

-- Replace the function with unambiguous column references
CREATE OR REPLACE FUNCTION recalculate_subsequent_balances(
    p_account_id BIGINT,
    p_transaction_date DATE,
    p_transaction_id BIGINT
) RETURNS VOID AS $$
DECLARE
    trans_record RECORD;
    running_total DECIMAL(12,2); -- Renamed from current_balance to avoid ambiguity
BEGIN
    -- Get the balance from the transaction just before the ones we need to recalculate
    SELECT COALESCE(running_balance, 0) INTO running_total
    FROM transactions
    WHERE account_id = p_account_id
      AND (transaction_date < p_transaction_date
           OR (transaction_date = p_transaction_date AND id <= p_transaction_id))
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1;

    -- If no transaction found, use account's initial balance
    -- Use table-qualified column reference to avoid ambiguity
    IF running_total IS NULL THEN
        SELECT COALESCE(accounts.current_balance, 0) INTO running_total
        FROM accounts
        WHERE accounts.id = p_account_id;
    END IF;

    -- Update all subsequent transactions
    FOR trans_record IN
        SELECT id, amount
        FROM transactions
        WHERE account_id = p_account_id
          AND (transaction_date > p_transaction_date
               OR (transaction_date = p_transaction_date AND id > p_transaction_id))
        ORDER BY transaction_date ASC, id ASC
    LOOP
        running_total := running_total + trans_record.amount;

        UPDATE transactions
        SET running_balance = running_total,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = trans_record.id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Add comment for documentation
COMMENT ON FUNCTION recalculate_subsequent_balances(BIGINT, DATE, BIGINT) IS 'Fixed version - recalculates running balances for subsequent transactions without ambiguous column references';