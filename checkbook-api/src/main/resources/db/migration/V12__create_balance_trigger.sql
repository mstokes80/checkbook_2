-- V12__create_balance_trigger.sql
-- Create triggers for automatic running balance calculation

-- Function to calculate running balance for a transaction
CREATE OR REPLACE FUNCTION calculate_running_balance(
    p_account_id BIGINT,
    p_transaction_date DATE,
    p_transaction_id BIGINT
) RETURNS DECIMAL(12,2) AS $$
DECLARE
    previous_balance DECIMAL(12,2) := 0;
    current_transaction_amount DECIMAL(12,2);
BEGIN
    -- Get the previous balance from the most recent transaction before this one
    -- (ordered by date, then by id for transactions on the same date)
    SELECT COALESCE(running_balance, 0) INTO previous_balance
    FROM transactions
    WHERE account_id = p_account_id
      AND (transaction_date < p_transaction_date
           OR (transaction_date = p_transaction_date AND id < p_transaction_id))
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1;

    -- If no previous transaction found, check account's initial balance
    IF previous_balance = 0 THEN
        SELECT COALESCE(current_balance, 0) INTO previous_balance
        FROM accounts
        WHERE id = p_account_id;

        -- If there are any transactions before this one, use the last one's balance
        IF EXISTS (
            SELECT 1 FROM transactions
            WHERE account_id = p_account_id
              AND (transaction_date < p_transaction_date
                   OR (transaction_date = p_transaction_date AND id < p_transaction_id))
        ) THEN
            SELECT COALESCE(running_balance, 0) INTO previous_balance
            FROM transactions
            WHERE account_id = p_account_id
              AND (transaction_date < p_transaction_date
                   OR (transaction_date = p_transaction_date AND id < p_transaction_id))
            ORDER BY transaction_date DESC, id DESC
            LIMIT 1;
        END IF;
    END IF;

    -- Get the current transaction amount
    SELECT amount INTO current_transaction_amount
    FROM transactions
    WHERE id = p_transaction_id;

    -- Calculate new running balance
    RETURN previous_balance + current_transaction_amount;
END;
$$ LANGUAGE plpgsql;

-- Function to recalculate running balances for all subsequent transactions
CREATE OR REPLACE FUNCTION recalculate_subsequent_balances(
    p_account_id BIGINT,
    p_transaction_date DATE,
    p_transaction_id BIGINT
) RETURNS VOID AS $$
DECLARE
    trans_record RECORD;
    current_balance DECIMAL(12,2);
BEGIN
    -- Get the balance from the transaction just before the ones we need to recalculate
    SELECT COALESCE(running_balance, 0) INTO current_balance
    FROM transactions
    WHERE account_id = p_account_id
      AND (transaction_date < p_transaction_date
           OR (transaction_date = p_transaction_date AND id <= p_transaction_id))
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1;

    -- If no transaction found, use account's initial balance
    IF current_balance IS NULL THEN
        SELECT COALESCE(current_balance, 0) INTO current_balance
        FROM accounts
        WHERE id = p_account_id;
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
        current_balance := current_balance + trans_record.amount;

        UPDATE transactions
        SET running_balance = current_balance,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = trans_record.id;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Trigger function for INSERT operations
CREATE OR REPLACE FUNCTION transaction_insert_trigger() RETURNS TRIGGER AS $$
BEGIN
    -- Calculate running balance for the new transaction
    NEW.running_balance := calculate_running_balance(NEW.account_id, NEW.transaction_date, NEW.id);

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

-- Trigger function for UPDATE operations
CREATE OR REPLACE FUNCTION transaction_update_trigger() RETURNS TRIGGER AS $$
BEGIN
    -- If the amount, date, or account changed, recalculate
    IF OLD.amount != NEW.amount OR OLD.transaction_date != NEW.transaction_date OR OLD.account_id != NEW.account_id THEN

        -- Handle account change (if transaction moved to different account)
        IF OLD.account_id != NEW.account_id THEN
            -- Recalculate balances for the old account
            PERFORM recalculate_subsequent_balances(OLD.account_id, OLD.transaction_date, OLD.id);

            -- Update old account's current balance
            UPDATE accounts
            SET current_balance = COALESCE((
                SELECT running_balance
                FROM transactions
                WHERE account_id = OLD.account_id
                ORDER BY transaction_date DESC, id DESC
                LIMIT 1
            ), current_balance),
            updated_at = CURRENT_TIMESTAMP
            WHERE id = OLD.account_id;
        END IF;

        -- Calculate new running balance for the updated transaction
        NEW.running_balance := calculate_running_balance(NEW.account_id, NEW.transaction_date, NEW.id);

        -- Recalculate balances for subsequent transactions in the new/same account
        PERFORM recalculate_subsequent_balances(NEW.account_id, NEW.transaction_date, NEW.id);

        -- Update account's current balance
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
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger function for DELETE operations
CREATE OR REPLACE FUNCTION transaction_delete_trigger() RETURNS TRIGGER AS $$
BEGIN
    -- Recalculate balances for all subsequent transactions
    PERFORM recalculate_subsequent_balances(OLD.account_id, OLD.transaction_date, OLD.id);

    -- Update account's current balance
    UPDATE accounts
    SET current_balance = COALESCE((
        SELECT running_balance
        FROM transactions
        WHERE account_id = OLD.account_id
        ORDER BY transaction_date DESC, id DESC
        LIMIT 1
    ), (SELECT current_balance FROM accounts WHERE id = OLD.account_id)),
    updated_at = CURRENT_TIMESTAMP
    WHERE id = OLD.account_id;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

-- Create the triggers
CREATE TRIGGER transaction_balance_insert_trigger
    AFTER INSERT ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION transaction_insert_trigger();

CREATE TRIGGER transaction_balance_update_trigger
    AFTER UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION transaction_update_trigger();

CREATE TRIGGER transaction_balance_delete_trigger
    AFTER DELETE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION transaction_delete_trigger();

-- Add comments for documentation
COMMENT ON FUNCTION calculate_running_balance(BIGINT, DATE, BIGINT) IS 'Calculates the running balance for a specific transaction based on previous transactions';
COMMENT ON FUNCTION recalculate_subsequent_balances(BIGINT, DATE, BIGINT) IS 'Recalculates running balances for all transactions after the specified transaction';
COMMENT ON FUNCTION transaction_insert_trigger() IS 'Trigger function to handle running balance calculation on transaction insert';
COMMENT ON FUNCTION transaction_update_trigger() IS 'Trigger function to handle running balance calculation on transaction update';
COMMENT ON FUNCTION transaction_delete_trigger() IS 'Trigger function to handle running balance calculation on transaction delete';