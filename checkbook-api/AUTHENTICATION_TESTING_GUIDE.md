# Authentication System Testing Guide

This guide provides step-by-step instructions for manually testing the User Authentication System implementation.

## Prerequisites

1. **PostgreSQL Database Setup**
   ```bash
   # Create database and user (run as postgres superuser)
   psql -U postgres
   CREATE DATABASE checkbook_db;
   CREATE USER checkbook_user WITH PASSWORD 'checkbook_password';
   GRANT ALL PRIVILEGES ON DATABASE checkbook_db TO checkbook_user;
   ```

2. **Start the Application**
   ```bash
   cd checkbook-api
   mvn spring-boot:run
   ```

3. **Verify Application is Running**
   - Application should start on port 8080
   - Check logs for "Started CheckbookApiApplication"
   - Flyway migrations should apply automatically

## API Endpoints Overview

Base URL: `http://localhost:8080/api`

### Public Endpoints (No Authentication Required)
- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh JWT token
- `POST /auth/forgot-password` - Initiate password reset
- `POST /auth/reset-password` - Reset password with token
- `GET /auth/validate-reset-token` - Validate reset token
- `GET /auth/health` - Health check

### Protected Endpoints (Authentication Required)
- `GET /auth/me` - Get current user info
- `PUT /auth/profile` - Update user profile
- `POST /auth/logout` - Logout user

## Manual Testing Steps

### 1. Health Check

**Request:**
```bash
curl -X GET http://localhost:8080/api/auth/health
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Authentication service is running",
  "data": null
}
```

### 2. User Registration

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "User registered successfully! You can now log in.",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "Test User",
    "emailVerified": false
  }
}
```

**Test Cases:**
- ✅ Valid registration data
- ❌ Duplicate username
- ❌ Duplicate email
- ❌ Invalid email format
- ❌ Password too short (< 6 characters)
- ❌ Missing required fields

### 3. User Login

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "Test User",
    "emailVerified": false
  }
}
```

**Test Cases:**
- ✅ Login with username
- ✅ Login with email
- ❌ Invalid username/email
- ❌ Wrong password
- ❌ Account locked (after 5 failed attempts)
- ❌ Disabled account

### 4. Access Protected Endpoint

**Request:**
```bash
# Replace TOKEN with actual JWT from login response
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "User information retrieved",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "Test User",
    "emailVerified": false
  }
}
```

### 5. Token Refresh

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "REFRESH_TOKEN_FROM_LOGIN"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "fullName": "Test User",
    "emailVerified": false
  }
}
```

### 6. Update Profile

**Request:**
```bash
curl -X PUT http://localhost:8080/api/auth/profile \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name",
    "email": "updated@example.com"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "updated@example.com",
    "fullName": "Updated Name",
    "emailVerified": false
  }
}
```

### 7. Password Reset Flow

#### Step 1: Initiate Password Reset
**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "If the email exists, a password reset link has been sent.",
  "data": null
}
```

#### Step 2: Check Database for Token
```sql
-- Connect to database
psql -U checkbook_user -d checkbook_db

-- Check for reset token
SELECT token, expires_at, is_used FROM password_reset_tokens WHERE user_id = 1 ORDER BY created_at DESC LIMIT 1;
```

#### Step 3: Validate Token
**Request:**
```bash
curl -X GET "http://localhost:8080/api/auth/validate-reset-token?token=RESET_TOKEN_FROM_DB"
```

#### Step 4: Reset Password
**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "RESET_TOKEN_FROM_DB",
    "newPassword": "newpassword123"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Password has been reset successfully. You can now log in with your new password.",
  "data": null
}
```

### 8. Logout

**Request:**
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

## Security Features Testing

### 1. Account Locking
1. Attempt to login with wrong password 5 times
2. Account should be locked after 5th attempt
3. Verify login fails with "Account is locked" message

### 2. JWT Expiration
1. Wait for 24 hours (or modify JWT expiration for testing)
2. Try to access protected endpoint with expired token
3. Should receive 401 Unauthorized response

### 3. Rate Limiting (Password Reset)
1. Request password reset 3+ times within an hour for same user
2. Should receive rate limiting error
3. Try from same IP with different emails 10+ times
4. Should receive IP-based rate limiting error

### 4. SQL Injection Protection
1. Try registration with malicious SQL in username: `'; DROP TABLE users; --`
2. Should be safely handled by prepared statements

## Database Verification

### Check User Creation
```sql
SELECT id, username, email, first_name, last_name, enabled, account_locked,
       failed_login_attempts, created_at
FROM users;
```

### Check Password Reset Tokens
```sql
SELECT user_id, token, expires_at, is_used, request_ip, created_at
FROM password_reset_tokens
ORDER BY created_at DESC;
```

### Check Migration Status
```sql
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

## Error Scenarios to Test

### Authentication Errors
- Missing Authorization header
- Invalid JWT format
- Expired JWT token
- JWT with invalid signature

### Validation Errors
- Registration with invalid email format
- Password shorter than 6 characters
- Username shorter than 3 characters
- Missing required fields

### Business Logic Errors
- Duplicate username registration
- Duplicate email registration
- Login with non-existent user
- Password reset with invalid email
- Password reset with expired token

## Performance Testing

### Load Testing with curl
```bash
# Test multiple concurrent registrations
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"user$i\",\"email\":\"user$i@example.com\",\"password\":\"password123\",\"firstName\":\"User\",\"lastName\":\"$i\"}" &
done
wait
```

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Verify PostgreSQL is running
   - Check connection details in application.yml
   - Ensure database and user exist

2. **Flyway Migration Failed**
   - Check database permissions
   - Verify SQL syntax in migration files
   - Check flyway_schema_history table

3. **JWT Signature Issues**
   - Verify JWT secret in application.yml
   - Ensure secret is at least 256 bits (32 characters)

4. **Email Service Issues**
   - Check SMTP configuration
   - Verify email server connectivity
   - Check application logs for email errors

5. **Authentication Failures**
   - Verify Spring Security configuration
   - Check JWT filter is properly configured
   - Ensure UserDetailsService is implemented correctly

## Next Steps

After successful testing of Task 2, you can proceed to:
1. **Task 3**: Frontend React Authentication Components
2. **Task 4**: Integration and Security Configuration
3. **Task 5**: Additional features and enhancements

This completes the Backend Spring Boot Authentication Implementation (Task 2).