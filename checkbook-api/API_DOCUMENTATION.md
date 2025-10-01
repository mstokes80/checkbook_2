# Checkbook API Documentation

## Overview
The Checkbook API provides endpoints for managing financial accounts, authentication, and multi-account permissions.

## Base URL
```
http://localhost:8080/api
```

## Authentication
All protected endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

## Account Management Endpoints

### 1. Get All Accessible Accounts
Retrieves all accounts the authenticated user owns or has permission to access.

```http
GET /accounts
```

**Headers:**
- `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "message": "Accounts retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Primary Checking",
      "description": "Main checking account",
      "accountType": "CHECKING",
      "bankName": "First National Bank",
      "accountNumberMasked": "****1234",
      "isShared": true,
      "isOwner": true,
      "currentBalance": 2500.00,
      "ownerName": "John Doe",
      "userPermission": null,
      "createdAt": "2025-09-30T10:00:00Z",
      "updatedAt": "2025-09-30T10:00:00Z"
    }
  ]
}
```

### 2. Create New Account
Creates a new financial account for the authenticated user.

```http
POST /accounts
```

**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "name": "Savings Account",
  "description": "Emergency fund savings",
  "accountType": "SAVINGS",
  "bankName": "Credit Union",
  "accountNumber": "9876543210",
  "isShared": false,
  "initialBalance": 1000.00
}
```

**Account Types:**
- `CHECKING`
- `SAVINGS`
- `CREDIT_CARD`
- `INVESTMENT`
- `CASH`
- `OTHER`

**Response:**
```json
{
  "success": true,
  "message": "Account created successfully",
  "data": {
    "id": 2,
    "name": "Savings Account",
    "description": "Emergency fund savings",
    "accountType": "SAVINGS",
    "bankName": "Credit Union",
    "accountNumberMasked": "****3210",
    "isShared": false,
    "isOwner": true,
    "currentBalance": 1000.00,
    "ownerName": "John Doe",
    "userPermission": null,
    "createdAt": "2025-09-30T10:05:00Z",
    "updatedAt": "2025-09-30T10:05:00Z"
  }
}
```

### 3. Get Account Details
Retrieves detailed information about a specific account.

```http
GET /accounts/{accountId}
```

**Headers:**
- `Authorization: Bearer <token>`

**Path Parameters:**
- `accountId` (required): The ID of the account

**Response:**
```json
{
  "success": true,
  "message": "Account retrieved successfully",
  "data": {
    "id": 1,
    "name": "Primary Checking",
    "description": "Main checking account",
    "accountType": "CHECKING",
    "bankName": "First National Bank",
    "accountNumberMasked": "****1234",
    "isShared": true,
    "isOwner": true,
    "currentBalance": 2500.00,
    "ownerName": "John Doe",
    "userPermission": null,
    "createdAt": "2025-09-30T10:00:00Z",
    "updatedAt": "2025-09-30T10:00:00Z"
  }
}
```

### 4. Update Account
Updates account information. Only account owners can update their accounts.

```http
PUT /accounts/{accountId}
```

**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Path Parameters:**
- `accountId` (required): The ID of the account

**Request Body:**
```json
{
  "name": "Updated Account Name",
  "description": "Updated description",
  "bankName": "New Bank Name",
  "isShared": true
}
```

**Response:**
```json
{
  "success": true,
  "message": "Account updated successfully",
  "data": {
    "id": 1,
    "name": "Updated Account Name",
    "description": "Updated description",
    "accountType": "CHECKING",
    "bankName": "New Bank Name",
    "accountNumberMasked": "****1234",
    "isShared": true,
    "isOwner": true,
    "currentBalance": 2500.00,
    "ownerName": "John Doe",
    "userPermission": null,
    "createdAt": "2025-09-30T10:00:00Z",
    "updatedAt": "2025-09-30T10:15:00Z"
  }
}
```

### 5. Delete Account
Deletes an account. Only account owners can delete their accounts.

```http
DELETE /accounts/{accountId}
```

**Headers:**
- `Authorization: Bearer <token>`

**Path Parameters:**
- `accountId` (required): The ID of the account

**Response:**
```json
{
  "success": true,
  "message": "Account deleted successfully",
  "data": null
}
```

## Permission Management Endpoints

### 6. Grant Account Permission
Grants permission to another user to access a shared account.

```http
POST /accounts/{accountId}/permissions
```

**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Path Parameters:**
- `accountId` (required): The ID of the account

**Request Body:**
```json
{
  "usernameOrEmail": "user@example.com",
  "permissionType": "VIEW_ONLY"
}
```

**Permission Types:**
- `VIEW_ONLY`: User can view account details but cannot modify
- `FULL_ACCESS`: User can view and modify account (future enhancement)

**Response:**
```json
{
  "success": true,
  "message": "Permission granted successfully",
  "data": {
    "id": 1,
    "accountId": 1,
    "accountName": "Primary Checking",
    "userId": 2,
    "username": "user@example.com",
    "email": "user@example.com",
    "fullName": "Jane Smith",
    "permissionType": "VIEW_ONLY"
  }
}
```

### 7. Revoke Account Permission
Revokes a user's permission to access a shared account.

```http
DELETE /accounts/{accountId}/permissions/{userId}
```

**Headers:**
- `Authorization: Bearer <token>`

**Path Parameters:**
- `accountId` (required): The ID of the account
- `userId` (required): The ID of the user whose permission to revoke

**Response:**
```json
{
  "success": true,
  "message": "Permission revoked successfully",
  "data": null
}
```

### 8. Get Account Permissions
Retrieves all permissions for a specific account.

```http
GET /accounts/{accountId}/permissions
```

**Headers:**
- `Authorization: Bearer <token>`

**Path Parameters:**
- `accountId` (required): The ID of the account

**Response:**
```json
{
  "success": true,
  "message": "Account permissions retrieved successfully",
  "data": [
    {
      "id": 1,
      "accountId": 1,
      "accountName": "Primary Checking",
      "userId": 2,
      "username": "user@example.com",
      "email": "user@example.com",
      "fullName": "Jane Smith",
      "permissionType": "VIEW_ONLY"
    }
  ]
}
```

### 9. Dashboard Data
Retrieves dashboard data showing account overview and statistics.

```http
GET /accounts/dashboard
```

**Headers:**
- `Authorization: Bearer <token>`

**Response:**
```json
{
  "success": true,
  "message": "Dashboard data retrieved successfully",
  "data": {
    "totalBalance": 3500.00,
    "totalAccounts": 2,
    "ownedAccounts": 2,
    "sharedAccounts": 0,
    "recentActivity": [
      {
        "accountId": 1,
        "accountName": "Primary Checking",
        "activity": "Account created",
        "timestamp": "2025-09-30T10:00:00Z"
      }
    ]
  }
}
```

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "success": false,
  "message": "Invalid request data",
  "data": null
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "You need to login first to access this resource",
  "data": null
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied. You don't have permission to access this resource",
  "data": null
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Resource not found",
  "data": null
}
```

### 429 Too Many Requests
```json
{
  "rateLimitInfo": {
    "window": "15 minutes",
    "resetIn": "10 minutes",
    "limit": 5
  },
  "path": "/api/accounts",
  "error": "Rate Limit Exceeded",
  "message": "Too many requests. Please try again later.",
  "timestamp": "2025-09-30T10:30:00Z",
  "status": 429
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "An internal server error occurred",
  "data": null
}
```

## Authentication Endpoints

For complete authentication documentation, see [AUTHENTICATION_TESTING_GUIDE.md](./AUTHENTICATION_TESTING_GUIDE.md).

### Key Authentication Endpoints:
- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh JWT token
- `POST /auth/logout` - User logout
- `POST /auth/forgot-password` - Request password reset
- `POST /auth/reset-password` - Reset password

## Rate Limiting

All endpoints are subject to rate limiting:
- **Login attempts**: 5 attempts per 15 minutes per IP
- **Other endpoints**: Standard rate limiting applies

## Data Validation

### Account Creation Validation:
- `name`: Required, 1-100 characters, cannot be empty/whitespace
- `accountType`: Must be one of valid enum values
- `bankName`: Optional, max 100 characters
- `accountNumber`: Optional, used to generate masked display
- `initialBalance`: Optional, defaults to 0.00
- `isShared`: Optional boolean, defaults to false

### Permission Management Validation:
- `usernameOrEmail`: Required, must reference existing user
- `permissionType`: Required, must be valid enum value
- Account must be marked as shared (`isShared: true`)
- Only account owners can grant/revoke permissions

## Security Features

1. **JWT Authentication**: All protected endpoints require valid JWT tokens
2. **Role-based Access Control**: Users can only access their own accounts or shared accounts with proper permissions
3. **Rate Limiting**: Prevents abuse and brute force attacks
4. **Data Validation**: Comprehensive input validation and sanitization
5. **Audit Logging**: All account operations are logged with timestamps and user information

## Database Schema

The multi-account system uses the following database tables:
- `users`: User accounts and authentication
- `accounts`: Financial account information
- `account_permissions`: Shared account access permissions

For detailed schema information, see the Flyway migration files in `src/main/resources/db/migration/`.