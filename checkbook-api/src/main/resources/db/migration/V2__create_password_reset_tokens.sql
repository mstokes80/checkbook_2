-- V2__create_password_reset_tokens.sql
-- Create password reset tokens table for secure password reset functionality

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,

    -- Security and tracking fields
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    is_used BOOLEAN NOT NULL DEFAULT false,

    -- Request tracking
    request_ip VARCHAR(45), -- IPv6 compatible
    user_agent TEXT,

    -- Audit fields
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_password_reset_tokens_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create JWT blacklist table for logout functionality
CREATE TABLE jwt_blacklist (
    id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(255) NOT NULL UNIQUE, -- JWT ID claim
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    blacklisted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(100), -- 'logout', 'security_breach', 'password_change', etc.

    -- Foreign key constraint
    CONSTRAINT fk_jwt_blacklist_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create user sessions table for session management
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    jti VARCHAR(255) NOT NULL UNIQUE, -- JWT ID for correlation

    -- Session details
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_info TEXT,

    -- Session lifecycle
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,

    -- Foreign key constraint
    CONSTRAINT fk_user_sessions_user_id
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create trigger to automatically update last_accessed_at
CREATE TRIGGER update_user_sessions_last_accessed
    BEFORE UPDATE ON user_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE password_reset_tokens IS 'Secure tokens for password reset functionality';
COMMENT ON COLUMN password_reset_tokens.token IS 'Cryptographically secure random token';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiration time (typically 1 hour from creation)';
COMMENT ON COLUMN password_reset_tokens.request_ip IS 'IP address that requested the reset';

COMMENT ON TABLE jwt_blacklist IS 'Blacklisted JWT tokens for logout and security';
COMMENT ON COLUMN jwt_blacklist.jti IS 'JWT ID claim for unique token identification';
COMMENT ON COLUMN jwt_blacklist.token_hash IS 'SHA-256 hash of the token for verification';
COMMENT ON COLUMN jwt_blacklist.reason IS 'Reason for blacklisting the token';

COMMENT ON TABLE user_sessions IS 'Active user sessions for device and security management';
COMMENT ON COLUMN user_sessions.jti IS 'JWT ID for correlation with JWT tokens';
COMMENT ON COLUMN user_sessions.device_info IS 'Device fingerprint or description';
COMMENT ON COLUMN user_sessions.last_accessed_at IS 'Last time the session was used';