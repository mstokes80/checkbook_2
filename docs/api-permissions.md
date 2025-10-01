# Account Permissions API Documentation

## Overview

The Account Permissions API provides comprehensive access control for shared accounts, allowing account owners to grant, modify, and revoke permissions for other users. The system supports three permission levels and includes a formal request/approval workflow.

## Permission Levels

| Level | Description | Capabilities |
|-------|-------------|--------------|
| `VIEW_ONLY` | Read-only access | View account details, transactions, and balance |
| `TRANSACTION_ONLY` | Transaction management | Everything in VIEW_ONLY plus add/edit/delete transactions |
| `FULL_ACCESS` | Administrative access | Everything in TRANSACTION_ONLY plus manage permissions (except ownership transfer) |

**Note:** Only account owners can manage permissions and ownership cannot be transferred.

## Authentication

All endpoints require Bearer token authentication:
```
Authorization: Bearer <jwt_token>
```

## Endpoints

### Account Permission Management

#### Get Account Permissions
```http
GET /api/accounts/{accountId}/permissions
```

**Authorization:** Account owner or users with account access

**Response:**
```json
{
  "success": true,
  "message": "Permissions retrieved successfully",
  "data": [
    {
      "id": 5,
      "accountId": 1,
      "accountName": "Matt's Checking",
      "userId": 2,
      "username": "robyn",
      "email": "snowkittenmeow@gmail.com",
      "fullName": "Robyn Stokes",
      "permissionType": "FULL_ACCESS",
      "createdAt": "2025-09-30T10:23:15.960435",
      "updatedAt": "2025-09-30T10:39:00.241453"
    }
  ]
}
```

#### Grant Permission
```http
POST /api/accounts/{accountId}/permissions
```

**Authorization:** Account owner only

**Request Body:**
```json
{
  "usernameOrEmail": "robyn",
  "permissionType": "VIEW_ONLY"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Permission granted successfully",
  "data": {
    "id": 5,
    "accountId": 1,
    "accountName": "Matt's Checking",
    "userId": 2,
    "username": "robyn",
    "email": "snowkittenmeow@gmail.com",
    "fullName": "Robyn Stokes",
    "permissionType": "VIEW_ONLY",
    "createdAt": "2025-09-30T10:23:15.960435",
    "updatedAt": "2025-09-30T10:23:15.960435"
  }
}
```

#### Update Permission
```http
PUT /api/accounts/{accountId}/permissions/{userId}
```

**Authorization:** Account owner only

**Request Body:**
```json
{
  "usernameOrEmail": "robyn",
  "permissionType": "TRANSACTION_ONLY"
}
```

**Response:** Same format as Grant Permission

#### Revoke Permission
```http
DELETE /api/accounts/{accountId}/permissions/{userId}
```

**Authorization:** Account owner only

**Response:**
```json
{
  "success": true,
  "message": "Permission revoked successfully",
  "data": null
}
```

### Permission Request Workflow

#### Create Permission Request
```http
POST /api/permission-requests
```

**Authorization:** Any authenticated user

**Request Body:**
```json
{
  "accountId": 1,
  "requestedPermission": "FULL_ACCESS",
  "reason": "I need to help manage the account and add other family members"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Permission request submitted successfully",
  "data": {
    "id": 1,
    "accountId": 1,
    "requesterId": 2,
    "requestedPermission": "FULL_ACCESS",
    "currentPermission": "TRANSACTION_ONLY",
    "requestMessage": null,
    "status": "PENDING",
    "reviewedBy": null,
    "reviewMessage": null,
    "createdAt": "2025-09-30T10:38:21.347072",
    "reviewedAt": null
  }
}
```

#### Get Pending Requests (Account Owner)
```http
GET /api/permission-requests/pending
```

**Authorization:** Account owner

**Response:**
```json
{
  "success": true,
  "message": "Pending permission requests retrieved successfully",
  "data": [
    {
      "id": 1,
      "accountId": 1,
      "requesterId": 2,
      "requestedPermission": "FULL_ACCESS",
      "currentPermission": "TRANSACTION_ONLY",
      "status": "PENDING",
      "createdAt": "2025-09-30T10:38:21.347072",
      "accountName": "Matt's Checking",
      "requesterUsername": "robyn",
      "requesterEmail": "snowkittenmeow@gmail.com",
      "requesterFullName": "Robyn Stokes"
    }
  ]
}
```

#### Approve Permission Request
```http
PUT /api/permission-requests/account/{accountId}/{requestId}/approve
```

**Authorization:** Account owner

**Request Body:**
```json
{
  "message": "Approved - you can help manage the account"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Permission request approved successfully",
  "data": {
    "id": 1,
    "accountId": 1,
    "requesterId": 2,
    "requestedPermission": "FULL_ACCESS",
    "currentPermission": "TRANSACTION_ONLY",
    "status": "APPROVED",
    "reviewedBy": 1,
    "reviewMessage": "Approved - you can help manage the account",
    "createdAt": "2025-09-30T10:38:21.347072",
    "reviewedAt": "2025-09-30T10:39:00.244762"
  }
}
```

#### Deny Permission Request
```http
PUT /api/permission-requests/account/{accountId}/{requestId}/deny
```

**Authorization:** Account owner

**Request Body:**
```json
{
  "message": "Not ready for full access yet"
}
```

**Response:** Same format as Approve with status "DENIED"

#### Get My Requests
```http
GET /api/permission-requests/my-requests?page=0&size=10
```

**Authorization:** Any authenticated user

**Response:** Paginated list of user's permission requests

#### Cancel Request
```http
PUT /api/permission-requests/{requestId}/cancel
```

**Authorization:** Request creator only

**Response:**
```json
{
  "success": true,
  "message": "Permission request cancelled successfully",
  "data": {
    "id": 1,
    "status": "CANCELLED",
    "reviewedAt": "2025-09-30T10:45:00.000000"
  }
}
```

### Audit Logging

#### Get Account Audit Logs
```http
GET /api/accounts/{accountId}/audit-logs?page=0&size=20&actionType=PERMISSION_MODIFIED&userId=2&startDate=2025-09-30T00:00:00&endDate=2025-09-30T23:59:59
```

**Authorization:** Account owner or users with account access

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `actionType` (optional): Filter by action type
- `userId` (optional): Filter by user ID
- `startDate` (optional): Filter by start date (ISO format)
- `endDate` (optional): Filter by end date (ISO format)

**Available Action Types:**
- `PERMISSION_GRANTED`
- `PERMISSION_MODIFIED`
- `PERMISSION_REVOKED`
- `PERMISSION_REQUESTED`
- `PERMISSION_REQUEST_APPROVED`
- `PERMISSION_REQUEST_DENIED`
- `ACCOUNT_VIEWED`
- `TRANSACTION_ADDED`
- `ACCOUNT_MODIFIED`

**Response:**
```json
{
  "success": true,
  "message": "Audit logs retrieved successfully",
  "data": {
    "content": [
      {
        "id": 9,
        "accountId": 1,
        "userId": 1,
        "actionType": "PERMISSION_REQUEST_APPROVED",
        "actionDetails": "{\"requestId\": 1, \"reviewMessage\": \"Approved - you can help manage the account\", \"permissionType\": \"FULL_ACCESS\", \"approvingUserId\": 1, \"requestingUserId\": 2, \"approvingUsername\": \"bandittr6@gmail.com\", \"requestingUsername\": \"robyn\"}",
        "ipAddress": "::1",
        "userAgent": "curl/8.7.1",
        "createdAt": "2025-09-30T10:39:00.251759"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {"sorted": true, "unsorted": false, "empty": false}
    },
    "totalElements": 8,
    "totalPages": 1,
    "last": true,
    "first": true,
    "numberOfElements": 5
  }
}
```

## Error Responses

### Common Error Codes

| Status Code | Error | Description |
|-------------|-------|-------------|
| 400 | Bad Request | Invalid request format or missing required fields |
| 401 | Unauthorized | Authentication required or token expired |
| 403 | Forbidden | User lacks permission for this operation |
| 404 | Not Found | Account, user, or permission request not found |
| 409 | Conflict | Permission already exists or duplicate request |
| 500 | Internal Server Error | Server-side error |

### Error Response Format
```json
{
  "timestamp": "2025-09-30T10:35:04",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You don't have permission to access this resource",
  "path": "/api/accounts/1/permissions",
  "requestId": "8c802e27"
}
```

### Validation Errors
```json
{
  "timestamp": "2025-09-30T10:35:24",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/accounts/1/permissions/2",
  "requestId": "80e09c47",
  "details": {
    "usernameOrEmail": "Username or email is required"
  }
}
```

## Workflow Examples

### Typical Permission Grant Flow
1. Account owner creates account (automatically becomes owner)
2. Owner shares account by granting VIEW_ONLY permission to family member
3. Family member can view account but cannot modify anything
4. Family member requests upgrade to TRANSACTION_ONLY via permission request
5. Owner approves request, permission automatically upgraded
6. Family member can now add/edit transactions
7. All actions logged in audit trail

### Permission Request Flow
1. User with existing permission requests upgrade: `POST /api/permission-requests`
2. Account owner sees pending request: `GET /api/permission-requests/pending`
3. Owner approves/denies: `PUT /api/permission-requests/account/{accountId}/{requestId}/approve`
4. Permission automatically updated on approval
5. User notified of decision
6. Complete audit trail maintained

## Security Considerations

- Only account owners can manage permissions
- Users can only request permissions for accounts they already have access to
- All permission changes are logged with IP address and user agent
- JWT tokens required for all operations
- Role-based access control enforced at API level
- Input validation prevents injection attacks
- Audit logs maintain complete accountability trail