-- V3__add_performance_indexes.sql
-- Create performance indexes for authentication system

-- Users table indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled_locked ON users(enabled, account_locked);
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_last_login ON users(last_successful_login_at);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Password reset tokens indexes
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_used ON password_reset_tokens(is_used, expires_at);

-- JWT blacklist indexes
CREATE INDEX idx_jwt_blacklist_jti ON jwt_blacklist(jti);
CREATE INDEX idx_jwt_blacklist_user_id ON jwt_blacklist(user_id);
CREATE INDEX idx_jwt_blacklist_expires_at ON jwt_blacklist(expires_at);

-- User sessions indexes
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token ON user_sessions(session_token);
CREATE INDEX idx_user_sessions_jti ON user_sessions(jti);
CREATE INDEX idx_user_sessions_active ON user_sessions(is_active, expires_at);
CREATE INDEX idx_user_sessions_last_accessed ON user_sessions(last_accessed_at);

-- Composite indexes for common queries
CREATE INDEX idx_users_login_lookup ON users(username, enabled, account_locked);
CREATE INDEX idx_users_email_login_lookup ON users(email, enabled, account_locked);
CREATE INDEX idx_password_reset_active ON password_reset_tokens(token, is_used, expires_at);
CREATE INDEX idx_user_sessions_active_lookup ON user_sessions(user_id, is_active, expires_at);

-- Add database-level constraints for data integrity
ALTER TABLE users ADD CONSTRAINT chk_users_username_length CHECK (length(username) >= 3);
ALTER TABLE users ADD CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
ALTER TABLE users ADD CONSTRAINT chk_users_password_hash_length CHECK (length(password_hash) >= 60);
ALTER TABLE users ADD CONSTRAINT chk_users_failed_attempts_positive CHECK (failed_login_attempts >= 0);

ALTER TABLE password_reset_tokens ADD CONSTRAINT chk_password_reset_expires_future CHECK (expires_at > created_at);
ALTER TABLE jwt_blacklist ADD CONSTRAINT chk_jwt_blacklist_expires_future CHECK (expires_at > blacklisted_at);
ALTER TABLE user_sessions ADD CONSTRAINT chk_user_sessions_expires_future CHECK (expires_at > created_at);

-- Create cleanup function for expired tokens and sessions
CREATE OR REPLACE FUNCTION cleanup_expired_auth_data()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER := 0;
    temp_count INTEGER;
BEGIN
    -- Clean up expired password reset tokens
    DELETE FROM password_reset_tokens
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 day';
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;

    -- Clean up expired JWT blacklist entries
    DELETE FROM jwt_blacklist
    WHERE expires_at < CURRENT_TIMESTAMP;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;

    -- Clean up expired user sessions
    DELETE FROM user_sessions
    WHERE expires_at < CURRENT_TIMESTAMP OR last_accessed_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Add comments for documentation
COMMENT ON FUNCTION cleanup_expired_auth_data() IS 'Function to clean up expired authentication-related data';

-- Note: In production, you would set up a cron job or scheduled task to call cleanup_expired_auth_data() regularly