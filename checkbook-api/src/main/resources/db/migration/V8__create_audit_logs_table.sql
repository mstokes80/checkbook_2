-- V8__create_audit_logs_table.sql
-- Create audit logs table for tracking permission changes and user activities

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_details JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_audit_logs_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Check constraints
    CONSTRAINT chk_action_type CHECK (action_type IN (
        'PERMISSION_GRANTED', 'PERMISSION_MODIFIED', 'PERMISSION_REVOKED',
        'ACCOUNT_VIEWED', 'TRANSACTION_ADDED', 'ACCOUNT_MODIFIED',
        'PERMISSION_REQUESTED', 'PERMISSION_REQUEST_APPROVED', 'PERMISSION_REQUEST_DENIED'
    ))
);

-- Create indexes for performance
CREATE INDEX idx_audit_logs_account_id ON audit_logs(account_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action_type ON audit_logs(action_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- Create GIN index for JSONB action_details for efficient querying
CREATE INDEX idx_audit_logs_action_details ON audit_logs USING GIN(action_details);

-- Add comments for documentation
COMMENT ON TABLE audit_logs IS 'Audit trail for account activities and permission changes';
COMMENT ON COLUMN audit_logs.account_id IS 'Reference to the account involved in the action';
COMMENT ON COLUMN audit_logs.user_id IS 'Reference to the user who performed the action';
COMMENT ON COLUMN audit_logs.action_type IS 'Type of action performed (permission changes, account access, etc.)';
COMMENT ON COLUMN audit_logs.action_details IS 'Additional details about the action in JSON format';
COMMENT ON COLUMN audit_logs.ip_address IS 'IP address from which the action was performed';
COMMENT ON COLUMN audit_logs.user_agent IS 'User agent string of the client';
COMMENT ON COLUMN audit_logs.created_at IS 'Timestamp when the action was performed';