-- V20__add_transaction_audit_action_types.sql
-- Allow TRANSACTION_MODIFIED and TRANSACTION_DELETED audit entries.
-- V8's chk_action_type omitted them, so every transaction update/delete failed its audit insert
-- and rolled back the whole request. Keep this list in sync with AuditLog.ActionType.

ALTER TABLE audit_logs DROP CONSTRAINT chk_action_type;

ALTER TABLE audit_logs ADD CONSTRAINT chk_action_type CHECK (action_type IN (
    'PERMISSION_GRANTED', 'PERMISSION_MODIFIED', 'PERMISSION_REVOKED',
    'ACCOUNT_VIEWED', 'TRANSACTION_ADDED', 'TRANSACTION_MODIFIED', 'TRANSACTION_DELETED',
    'ACCOUNT_MODIFIED',
    'PERMISSION_REQUESTED', 'PERMISSION_REQUEST_APPROVED', 'PERMISSION_REQUEST_DENIED'
));
