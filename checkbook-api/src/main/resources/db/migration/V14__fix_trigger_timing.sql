-- V14__fix_trigger_timing.sql
-- Fix the trigger timing issue where NEW.id is NULL in BEFORE INSERT triggers

-- Drop the current BEFORE INSERT trigger that has the ID issue
DROP TRIGGER IF EXISTS transaction_balance_insert_trigger ON transactions;

-- Create new AFTER INSERT trigger for initial running balance calculation
CREATE OR REPLACE FUNCTION transaction_after_insert_balance_trigger() RETURNS TRIGGER AS $$
BEGIN
    -- Calculate running balance for the new transaction
    -- Now NEW.id is available since this is AFTER INSERT
    UPDATE transactions
    SET running_balance = calculate_running_balance(NEW.account_id, NEW.transaction_date, NEW.id),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.id;

    -- Recalculate balances for all subsequent transactions
    PERFORM recalculate_subsequent_balances(NEW.account_id, NEW.transaction_date, NEW.id);

    -- Update account's current balance to match the latest transaction
    UPDATE accounts
    SET current_balance = (
        SELECT running_balance
        FROM transactions
        WHERE account_id = NEW.account_id
        ORDER BY transaction_date DESC, id DESC
        LIMIT 1
    ),
    updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.account_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create the AFTER INSERT trigger
CREATE TRIGGER transaction_balance_after_insert_trigger
    AFTER INSERT ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION transaction_after_insert_balance_trigger();

-- Update the existing BEFORE INSERT trigger to just set a default value
CREATE OR REPLACE FUNCTION transaction_before_insert_trigger() RETURNS TRIGGER AS $$
BEGIN
    -- Set a default running balance value to satisfy NOT NULL constraint
    -- The real value will be calculated by the AFTER INSERT trigger
    IF NEW.running_balance IS NULL THEN
        NEW.running_balance := 0;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create the BEFORE INSERT trigger for setting defaults
CREATE TRIGGER transaction_balance_before_insert_trigger
    BEFORE INSERT ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION transaction_before_insert_trigger();

-- Add comments
COMMENT ON FUNCTION transaction_after_insert_balance_trigger() IS 'AFTER INSERT trigger - calculates running balance when ID is available';
COMMENT ON FUNCTION transaction_before_insert_trigger() IS 'BEFORE INSERT trigger - sets default running balance to satisfy NOT NULL constraint';