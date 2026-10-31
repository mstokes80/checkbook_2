-- V21__rebuild_balance_calculation.sql
-- Replace the incremental running-balance triggers (V12-V18) with a full per-account recalculation.
--
-- The old triggers used accounts.current_balance as both the opening balance and the latest balance,
-- so backdated inserts and deletes double-counted, and the AFTER UPDATE trigger's
-- NEW.running_balance assignment was silently discarded, leaving edited rows stale.
--
-- Now accounts.opening_balance holds the starting balance, and every write recalculates:
--   running_balance = opening_balance + SUM(amount) over (transaction_date, id)
--   current_balance = opening_balance + SUM(amount)

ALTER TABLE accounts ADD COLUMN opening_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00;

-- Backfill so each account keeps the current_balance it shows today
UPDATE accounts a
SET opening_balance = a.current_balance - COALESCE(
    (SELECT SUM(t.amount) FROM transactions t WHERE t.account_id = a.id), 0);

COMMENT ON COLUMN accounts.opening_balance IS 'Balance before any transactions; running balances are calculated from it';

-- Drop the old balance triggers and functions
DROP TRIGGER IF EXISTS transaction_balance_after_insert_trigger ON transactions;
DROP TRIGGER IF EXISTS transaction_balance_update_trigger ON transactions;
DROP TRIGGER IF EXISTS transaction_balance_delete_trigger ON transactions;

DROP FUNCTION IF EXISTS transaction_after_insert_balance_trigger();
DROP FUNCTION IF EXISTS transaction_insert_trigger();
DROP FUNCTION IF EXISTS transaction_update_trigger();
DROP FUNCTION IF EXISTS transaction_delete_trigger();
DROP FUNCTION IF EXISTS recalculate_subsequent_balances(BIGINT, DATE, BIGINT);
DROP FUNCTION IF EXISTS calculate_running_balance(BIGINT, DATE, BIGINT);

-- Recalculate every running balance and the current balance for one account
CREATE OR REPLACE FUNCTION recalculate_account_balances(p_account_id BIGINT) RETURNS VOID AS $$
DECLARE
    v_opening_balance DECIMAL(12,2);
BEGIN
    -- Lock the account row so concurrent writes to the same account recalculate one at a time.
    -- NO KEY UPDATE, not UPDATE: inserting a transaction already holds KEY SHARE on this row for
    -- its foreign key, and FOR UPDATE conflicts with that, deadlocking concurrent inserts.
    SELECT opening_balance INTO v_opening_balance
    FROM accounts
    WHERE id = p_account_id
    FOR NO KEY UPDATE;

    -- Account is gone (e.g. its transactions are being cascade-deleted)
    IF NOT FOUND THEN
        RETURN;
    END IF;

    UPDATE transactions t
    SET running_balance = v_opening_balance + ordered.cumulative_amount
    FROM (
        SELECT id, SUM(amount) OVER (ORDER BY transaction_date, id) AS cumulative_amount
        FROM transactions
        WHERE account_id = p_account_id
    ) ordered
    WHERE t.id = ordered.id
      AND t.running_balance IS DISTINCT FROM v_opening_balance + ordered.cumulative_amount;

    UPDATE accounts
    SET current_balance = v_opening_balance + COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE account_id = p_account_id), 0),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_account_id;
END;
$$ LANGUAGE plpgsql;

-- Trigger function for INSERT, UPDATE and DELETE on transactions
CREATE OR REPLACE FUNCTION transaction_balance_trigger() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM recalculate_account_balances(NEW.account_id);
    ELSIF TG_OP = 'DELETE' THEN
        PERFORM recalculate_account_balances(OLD.account_id);
    ELSE
        PERFORM recalculate_account_balances(NEW.account_id);
        -- Transaction moved to a different account
        IF OLD.account_id <> NEW.account_id THEN
            PERFORM recalculate_account_balances(OLD.account_id);
        END IF;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transaction_balance_insert_trigger
    AFTER INSERT ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION transaction_balance_trigger();

-- The WHEN clause also stops the running_balance updates made by recalculate_account_balances from re-firing it
CREATE TRIGGER transaction_balance_update_trigger
    AFTER UPDATE ON transactions
    FOR EACH ROW
    WHEN (OLD.amount IS DISTINCT FROM NEW.amount
          OR OLD.transaction_date IS DISTINCT FROM NEW.transaction_date
          OR OLD.account_id IS DISTINCT FROM NEW.account_id)
    EXECUTE FUNCTION transaction_balance_trigger();

CREATE TRIGGER transaction_balance_delete_trigger
    AFTER DELETE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION transaction_balance_trigger();

-- Keep opening_balance in step with balances set directly on the account:
-- account creation (initial balance) and PATCH /accounts/{id}/balance (reconcile to a known balance).
-- Recalculation writes current_balance = opening_balance + SUM(amount), so it derives the same value back.
CREATE OR REPLACE FUNCTION account_opening_balance_trigger() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.opening_balance := NEW.current_balance;
    ELSIF NEW.current_balance IS DISTINCT FROM OLD.current_balance THEN
        NEW.opening_balance := NEW.current_balance - COALESCE(
            (SELECT SUM(amount) FROM transactions WHERE account_id = NEW.id), 0);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER account_opening_balance_trigger
    BEFORE INSERT OR UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION account_opening_balance_trigger();

-- A new opening balance shifts every running balance
CREATE OR REPLACE FUNCTION account_opening_balance_changed_trigger() RETURNS TRIGGER AS $$
BEGIN
    PERFORM recalculate_account_balances(NEW.id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Not UPDATE OF opening_balance: that ignores changes made by the BEFORE trigger above
CREATE TRIGGER account_opening_balance_changed_trigger
    AFTER UPDATE ON accounts
    FOR EACH ROW
    WHEN (OLD.opening_balance IS DISTINCT FROM NEW.opening_balance)
    EXECUTE FUNCTION account_opening_balance_changed_trigger();

-- Repair running balances left wrong by the old triggers
SELECT recalculate_account_balances(id) FROM accounts;

COMMENT ON FUNCTION recalculate_account_balances(BIGINT) IS 'Recalculates every running balance and the current balance of an account from its opening balance';
COMMENT ON FUNCTION transaction_balance_trigger() IS 'Recalculates account balances after a transaction is inserted, updated or deleted';
COMMENT ON FUNCTION account_opening_balance_trigger() IS 'Derives opening_balance when current_balance is set directly on an account';
COMMENT ON FUNCTION account_opening_balance_changed_trigger() IS 'Recalculates running balances after an account''s opening balance changes';
