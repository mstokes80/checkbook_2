-- V4__fix_performance_indexes.sql
-- Fix the indexes that failed in V3 due to CURRENT_TIMESTAMP immutability issue

-- Since we can't use CURRENT_TIMESTAMP in index predicates (it's not immutable),
-- we'll create regular indexes instead. The cleanup function will handle expired data efficiently.

-- Note: The indexes that failed in V3 would have been:
-- CREATE INDEX idx_jwt_blacklist_cleanup ON jwt_blacklist(expires_at) WHERE expires_at < CURRENT_TIMESTAMP;
-- CREATE INDEX idx_user_sessions_cleanup ON user_sessions(expires_at) WHERE expires_at < CURRENT_TIMESTAMP;

-- Instead, we rely on the regular expires_at indexes and the cleanup function for performance.
-- The cleanup function will use the existing idx_jwt_blacklist_expires_at and
-- idx_user_sessions_active indexes for efficient expired data removal.

-- Add a comment explaining the design decision
COMMENT ON INDEX idx_jwt_blacklist_expires_at IS 'Used by cleanup_expired_auth_data() function for efficient expired token removal';
COMMENT ON INDEX idx_user_sessions_active IS 'Used by cleanup_expired_auth_data() function for efficient expired session removal';

-- Optional: Add a scheduled cleanup procedure comment
COMMENT ON FUNCTION cleanup_expired_auth_data() IS 'Function to clean up expired authentication data. Should be called periodically via cron job or scheduler. Uses existing indexes for efficient cleanup.';

-- Create a helper view for monitoring expired data (optional, for administrative purposes)
CREATE VIEW v_expired_auth_summary AS
SELECT
    'password_reset_tokens' as table_name,
    COUNT(*) as expired_count,
    MIN(expires_at) as oldest_expired,
    MAX(expires_at) as newest_expired
FROM password_reset_tokens
WHERE expires_at < CURRENT_TIMESTAMP
UNION ALL
SELECT
    'jwt_blacklist' as table_name,
    COUNT(*) as expired_count,
    MIN(expires_at) as oldest_expired,
    MAX(expires_at) as newest_expired
FROM jwt_blacklist
WHERE expires_at < CURRENT_TIMESTAMP
UNION ALL
SELECT
    'user_sessions' as table_name,
    COUNT(*) as expired_count,
    MIN(expires_at) as oldest_expired,
    MAX(expires_at) as newest_expired
FROM user_sessions
WHERE expires_at < CURRENT_TIMESTAMP OR last_accessed_at < CURRENT_TIMESTAMP - INTERVAL '30 days';

COMMENT ON VIEW v_expired_auth_summary IS 'Administrative view to monitor expired authentication data that needs cleanup';