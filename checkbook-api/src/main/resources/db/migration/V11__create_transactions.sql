-- V11__create_transactions.sql
-- Create transactions table for recording financial transactions

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    transaction_date DATE NOT NULL,
    category_id BIGINT,
    notes TEXT,
    running_balance DECIMAL(12, 2) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Foreign key constraints
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES transaction_categories(id) ON DELETE SET NULL,

    -- Check constraints for data validation
    CONSTRAINT chk_amount_not_zero CHECK (amount != 0),
    CONSTRAINT chk_description_not_empty CHECK (trim(description) != ''),
    CONSTRAINT chk_transaction_date_valid CHECK (transaction_date <= CURRENT_DATE + INTERVAL '30 days'),
    CONSTRAINT chk_amount_precision CHECK (amount >= -999999999999.99 AND amount <= 999999999999.99),
    CONSTRAINT chk_running_balance_precision CHECK (running_balance >= -999999999999.99 AND running_balance <= 999999999999.99)
);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create performance indexes for efficient querying
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_transaction_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);
CREATE INDEX idx_transactions_amount ON transactions(amount);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- Composite indexes for common query patterns
CREATE INDEX idx_transactions_account_date ON transactions(account_id, transaction_date);
CREATE INDEX idx_transactions_account_category ON transactions(account_id, category_id);

-- Unique index to ensure proper ordering for running balance calculation
CREATE UNIQUE INDEX idx_transactions_account_date_id ON transactions(account_id, transaction_date, id);

-- Add comments for documentation
COMMENT ON TABLE transactions IS 'Financial transactions with automatic running balance calculation';
COMMENT ON COLUMN transactions.account_id IS 'Reference to the account this transaction belongs to';
COMMENT ON COLUMN transactions.amount IS 'Transaction amount (positive for credits, negative for debits)';
COMMENT ON COLUMN transactions.description IS 'Human-readable description of the transaction';
COMMENT ON COLUMN transactions.transaction_date IS 'Date when the transaction occurred';
COMMENT ON COLUMN transactions.category_id IS 'Optional reference to transaction category for organization';
COMMENT ON COLUMN transactions.notes IS 'Additional notes or details about the transaction';
COMMENT ON COLUMN transactions.running_balance IS 'Account balance after this transaction (automatically calculated)';
COMMENT ON COLUMN transactions.created_by IS 'Username of the user who created this transaction';
COMMENT ON COLUMN transactions.updated_by IS 'Username of the user who last updated this transaction';