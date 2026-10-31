-- V10__create_transaction_categories.sql
-- Create transaction categories table for categorizing transactions

CREATE TABLE transaction_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_system_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT uq_transaction_categories_name UNIQUE (name),
    CONSTRAINT chk_category_name_not_empty CHECK (trim(name) != '')
);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_transaction_categories_updated_at
    BEFORE UPDATE ON transaction_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for performance
CREATE INDEX idx_transaction_categories_name ON transaction_categories(name);
CREATE INDEX idx_transaction_categories_is_system_default ON transaction_categories(is_system_default);
CREATE INDEX idx_transaction_categories_created_at ON transaction_categories(created_at);

-- Insert default system categories
INSERT INTO transaction_categories (name, is_system_default) VALUES
    ('Income', true),
    ('Food & Dining', true),
    ('Transportation', true),
    ('Utilities', true),
    ('Shopping', true),
    ('Healthcare', true),
    ('Entertainment', true),
    ('Education', true),
    ('Personal Care', true),
    ('Taxes', true),
    ('Insurance', true),
    ('Other', true),
    ('Uncategorized', true);

-- Add comments for documentation
COMMENT ON TABLE transaction_categories IS 'Categories for organizing and classifying transactions';
COMMENT ON COLUMN transaction_categories.name IS 'Human-readable category name (e.g., Food & Dining, Transportation)';
COMMENT ON COLUMN transaction_categories.is_system_default IS 'Whether this is a system-provided default category';
COMMENT ON COLUMN transaction_categories.created_at IS 'Timestamp when the category was created';
COMMENT ON COLUMN transaction_categories.updated_at IS 'Timestamp when the category was last updated';