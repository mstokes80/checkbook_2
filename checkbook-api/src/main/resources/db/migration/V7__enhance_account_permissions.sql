-- V7__enhance_account_permissions.sql
-- Enhance account permissions to support TRANSACTION_ONLY permission type

-- Drop existing permission type constraint
ALTER TABLE account_permissions
DROP CONSTRAINT chk_permission_type;

-- Add new constraint with TRANSACTION_ONLY permission type
ALTER TABLE account_permissions
ADD CONSTRAINT chk_permission_type
CHECK (permission_type IN ('VIEW_ONLY', 'TRANSACTION_ONLY', 'FULL_ACCESS'));

-- Add comment for documentation
COMMENT ON COLUMN account_permissions.permission_type IS 'Type of permission granted: VIEW_ONLY (read-only access), TRANSACTION_ONLY (can view and add transactions), FULL_ACCESS (complete access except permission management)';

-- Create index for permission hierarchy queries
CREATE INDEX idx_account_permissions_permission_level ON account_permissions(account_id, permission_type);