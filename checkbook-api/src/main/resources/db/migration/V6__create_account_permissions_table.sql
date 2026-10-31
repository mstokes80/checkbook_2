-- V6__create_account_permissions_table.sql
-- Create account_permissions table for shared account access control

CREATE TABLE account_permissions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    permission_type VARCHAR(20) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),

    -- Foreign key constraints
    CONSTRAINT fk_account_permissions_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_permissions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Unique constraint to prevent duplicate permissions
    CONSTRAINT uk_account_permissions_account_user UNIQUE (account_id, user_id),

    -- Check constraints
    CONSTRAINT chk_permission_type CHECK (permission_type IN ('VIEW_ONLY', 'FULL_ACCESS'))
);

-- Create trigger to automatically update updated_at timestamp
CREATE TRIGGER update_account_permissions_updated_at
    BEFORE UPDATE ON account_permissions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create indexes for performance
CREATE INDEX idx_account_permissions_account_id ON account_permissions(account_id);
CREATE INDEX idx_account_permissions_user_id ON account_permissions(user_id);
CREATE INDEX idx_account_permissions_permission_type ON account_permissions(permission_type);
CREATE INDEX idx_account_permissions_created_at ON account_permissions(created_at);

-- Add comments for documentation
COMMENT ON TABLE account_permissions IS 'Permissions for users to access shared accounts';
COMMENT ON COLUMN account_permissions.account_id IS 'Reference to the shared account';
COMMENT ON COLUMN account_permissions.user_id IS 'Reference to the user being granted permission';
COMMENT ON COLUMN account_permissions.permission_type IS 'Type of permission granted (VIEW_ONLY or FULL_ACCESS)';