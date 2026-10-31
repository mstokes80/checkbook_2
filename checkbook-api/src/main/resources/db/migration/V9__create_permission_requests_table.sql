-- V9__create_permission_requests_table.sql
-- Create permission requests table for managing permission escalation workflow

CREATE TABLE permission_requests (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    requested_permission VARCHAR(20) NOT NULL,
    current_permission VARCHAR(20),
    request_message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    review_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,

    -- Foreign key constraints
    CONSTRAINT fk_permission_requests_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_permission_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_permission_requests_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,

    -- Check constraints
    CONSTRAINT chk_requested_permission CHECK (requested_permission IN ('VIEW_ONLY', 'TRANSACTION_ONLY', 'FULL_ACCESS')),
    CONSTRAINT chk_current_permission CHECK (current_permission IN ('VIEW_ONLY', 'TRANSACTION_ONLY', 'FULL_ACCESS')),
    CONSTRAINT chk_request_status CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'CANCELLED')),

    -- Unique constraint to prevent duplicate pending requests
    CONSTRAINT uk_permission_requests_account_requester_pending UNIQUE (account_id, requester_id, status) DEFERRABLE INITIALLY DEFERRED
);

-- Create indexes for performance
CREATE INDEX idx_permission_requests_account_id ON permission_requests(account_id);
CREATE INDEX idx_permission_requests_requester_id ON permission_requests(requester_id);
CREATE INDEX idx_permission_requests_status ON permission_requests(status);
CREATE INDEX idx_permission_requests_created_at ON permission_requests(created_at);
CREATE INDEX idx_permission_requests_reviewed_by ON permission_requests(reviewed_by);

-- Create partial index for pending requests only
CREATE INDEX idx_permission_requests_pending ON permission_requests(account_id, requester_id) WHERE status = 'PENDING';

-- Add comments for documentation
COMMENT ON TABLE permission_requests IS 'Permission escalation requests for shared accounts';
COMMENT ON COLUMN permission_requests.account_id IS 'Reference to the account for which permission is requested';
COMMENT ON COLUMN permission_requests.requester_id IS 'Reference to the user requesting permission escalation';
COMMENT ON COLUMN permission_requests.requested_permission IS 'The permission level being requested';
COMMENT ON COLUMN permission_requests.current_permission IS 'The current permission level of the requester';
COMMENT ON COLUMN permission_requests.request_message IS 'Optional message from the requester explaining the need';
COMMENT ON COLUMN permission_requests.status IS 'Current status of the request (PENDING, APPROVED, DENIED, CANCELLED)';
COMMENT ON COLUMN permission_requests.reviewed_by IS 'Reference to the user who reviewed the request';
COMMENT ON COLUMN permission_requests.review_message IS 'Optional message from the reviewer explaining the decision';
COMMENT ON COLUMN permission_requests.created_at IS 'Timestamp when the request was created';
COMMENT ON COLUMN permission_requests.reviewed_at IS 'Timestamp when the request was reviewed';