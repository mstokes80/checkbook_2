-- V5__create_accounts_table.sql
-- Create accounts table for multi-account management

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    account_type VARCHAR(20) NOT NULL DEFAULT 'CHECKING',
    bank_name VARCHAR(100),
    account_number_masked VARCHAR(20),
    is_shared BOOLEAN NOT NULL DEFAULT false,
    owner_id BIGINT NOT NULL,
    current_balance DECIMAL(12, 2) NOT NULL DEFAULT 0.00,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Foreign key constraints
    CONSTRAINT fk_accounts_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT chk_account_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT', 'CASH', 'OTHER')),
    CONSTRAINT chk_account_name_not_empty CHECK (trim(name) != ''),
    CONSTRAINT chk_account_balance_precision CHECK (current_balance >= -999999999999.99 AND current_balance <= 999999999999.99)
);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_accounts_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for performance
CREATE INDEX idx_accounts_owner_id ON accounts(owner_id);
CREATE INDEX idx_accounts_is_shared ON accounts(is_shared);
CREATE INDEX idx_accounts_account_type ON accounts(account_type);
CREATE INDEX idx_accounts_created_at ON accounts(created_at);

-- Add comments for documentation
COMMENT ON TABLE accounts IS 'User financial accounts for tracking balances and transactions';
COMMENT ON COLUMN accounts.name IS 'User-defined name for the account';
COMMENT ON COLUMN accounts.description IS 'Optional description of the account';
COMMENT ON COLUMN accounts.account_type IS 'Type of account (CHECKING, SAVINGS, etc.)';
COMMENT ON COLUMN accounts.bank_name IS 'Name of the financial institution';
COMMENT ON COLUMN accounts.account_number_masked IS 'Masked account number for display (e.g., ***1234)';
COMMENT ON COLUMN accounts.is_shared IS 'Whether this account can be shared with other users';
COMMENT ON COLUMN accounts.owner_id IS 'User who owns this account';
COMMENT ON COLUMN accounts.current_balance IS 'Current balance of the account';