# Account Permissions Developer Guide

## Architecture Overview

The Account Permissions system is built on a multi-layered architecture providing secure, role-based access control for shared accounts. The system integrates Spring Security with custom authorization logic and comprehensive audit logging.

### Core Components

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   React UI      │    │   Spring Boot    │    │   PostgreSQL    │
│                 │    │                  │    │                 │
│ PermissionGuard │◄──►│ AccountController│◄──►│ account_perms   │
│ Components      │    │ Security Layer   │    │ permission_reqs │
│                 │    │ Audit Service    │    │ audit_logs      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

## Database Schema

### Core Tables

#### account_permissions
```sql
CREATE TABLE account_permissions (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    permission_type VARCHAR(20) NOT NULL CHECK (permission_type IN ('VIEW_ONLY', 'TRANSACTION_ONLY', 'FULL_ACCESS')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(account_id, user_id)
);
```

#### permission_requests
```sql
CREATE TABLE permission_requests (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    requester_id BIGINT NOT NULL REFERENCES users(id),
    requested_permission VARCHAR(20) NOT NULL,
    current_permission VARCHAR(20),
    request_message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT REFERENCES users(id),
    review_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
);
```

#### audit_logs
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    action_type VARCHAR(50) NOT NULL,
    action_details JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Permission Hierarchy

### Enum Definition
```java
public enum PermissionType {
    VIEW_ONLY("VIEW_ONLY", 1),
    TRANSACTION_ONLY("TRANSACTION_ONLY", 2),
    FULL_ACCESS("FULL_ACCESS", 3);

    private final String value;
    private final int level;

    public boolean canView() { return level >= 1; }
    public boolean canManageTransactions() { return level >= 2; }
    public boolean canManageNonOwnerPermissions() { return level >= 3; }
}
```

### Hierarchy Rules
- Each level inherits capabilities from lower levels
- Only account owners can manage permissions and ownership
- Permission upgrades require owner approval through request workflow
- All permission changes are audited

## Spring Security Integration

### Method-Level Security

The system uses `@PreAuthorize` annotations with custom security expressions:

```java
@RestController
public class AccountController {

    @GetMapping("/api/accounts/{accountId}")
    @PreAuthorize("@accountSecurity.hasAccountAccess(authentication, #accountId)")
    public ResponseEntity<ApiResponse> getAccount(@PathVariable Long accountId) {
        // Any user with account access can view
    }

    @PostMapping("/api/accounts/{accountId}/permissions")
    @PreAuthorize("@accountSecurity.canManageAccountPermissions(authentication, #accountId)")
    public ResponseEntity<ApiResponse> grantPermission(@PathVariable Long accountId) {
        // Only account owners can manage permissions
    }
}
```

### Custom Security Evaluator

The `AccountSecurityEvaluator` provides centralized authorization logic:

```java
@Component("accountSecurity")
public class AccountSecurityEvaluator {

    public boolean hasAccountAccess(Authentication auth, Long accountId) {
        User user = getCurrentUser(auth);
        Account account = accountRepository.findById(accountId).orElse(null);

        if (account == null) return false;

        // Owner always has access
        if (account.isOwner(user)) return true;

        // Check if user has any permission
        if (account.getIsShared()) {
            return accountPermissionRepository
                .existsByAccountIdAndUserId(accountId, user.getId());
        }

        return false;
    }

    public boolean canManageAccountPermissions(Authentication auth, Long accountId) {
        User user = getCurrentUser(auth);
        Account account = accountRepository.findById(accountId).orElse(null);

        // Only owners can manage permissions
        return account != null && account.isOwner(user);
    }
}
```

## Service Layer

### AccountService
Handles core account operations with permission validation:

```java
@Service
@Transactional
public class AccountService {

    @Autowired private AuditLogService auditLogService;
    @Autowired private PermissionValidationService permissionValidationService;

    public ApiResponse grantPermission(Long accountId, AccountPermissionRequest request, User currentUser) {
        // Validate owner permissions
        if (!permissionValidationService.canManagePermissions(currentUser, accountId)) {
            return ApiResponse.error("Access denied");
        }

        // Create permission
        AccountPermission permission = createPermission(accountId, request);

        // Log the action
        auditLogService.logPermissionGranted(accountId, currentUser,
            targetUser, request.getPermissionType(), additionalDetails);

        return ApiResponse.success("Permission granted successfully", permission);
    }
}
```

### AuditLogService
Provides comprehensive activity logging:

```java
@Service
public class AuditLogService {

    public void logPermissionGranted(Long accountId, User grantingUser,
                                   User targetUser, String permissionType,
                                   Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("grantingUserId", grantingUser.getId());
        details.put("grantingUsername", grantingUser.getUsername());
        details.put("targetUserId", targetUser.getId());
        details.put("targetUsername", targetUser.getUsername());
        details.put("permissionType", permissionType);

        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        createAuditLog(accountId, grantingUser.getId(),
                      AuditLog.ActionType.PERMISSION_GRANTED, details);
    }

    private void createAuditLog(Long accountId, Long userId,
                               AuditLog.ActionType actionType,
                               Map<String, Object> details) {
        try {
            // Capture HTTP context
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            String detailsJson = objectMapper.writeValueAsString(details);
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            AuditLog auditLog = new AuditLog(accountId, userId, actionType,
                                           detailsJson, ipAddress, userAgent);
            auditLogRepository.save(auditLog);

        } catch (Exception e) {
            logger.error("Failed to create audit log: {}", e.getMessage());
            // Still save a basic log without details
            AuditLog auditLog = new AuditLog(accountId, userId, actionType, null, null, null);
            auditLogRepository.save(auditLog);
        }
    }
}
```

### Permission Request Workflow

The `PermissionRequestService` handles the request/approval workflow:

```java
@Service
@Transactional
public class PermissionRequestService {

    public ApiResponse createPermissionRequest(Long accountId,
                                             PermissionRequestDto request,
                                             User currentUser) {
        // Validate user has existing access
        if (!permissionValidationService.hasAccountAccess(currentUser, accountId)) {
            return ApiResponse.error("You don't have access to this account");
        }

        // Check for existing pending request
        Optional<PermissionRequest> existing = permissionRequestRepository
            .findByAccountIdAndRequesterIdAndStatus(accountId, currentUser.getId(),
                                                   PermissionRequest.RequestStatus.PENDING);
        if (existing.isPresent()) {
            return ApiResponse.error("You already have a pending request for this account");
        }

        // Create new request
        PermissionRequest permissionRequest = new PermissionRequest();
        permissionRequest.setAccountId(accountId);
        permissionRequest.setRequesterId(currentUser.getId());
        permissionRequest.setRequestedPermission(request.getRequestedPermission());
        permissionRequest.setCurrentPermission(getCurrentPermission(currentUser, accountId));
        permissionRequest.setRequestMessage(request.getReason());

        permissionRequest = permissionRequestRepository.save(permissionRequest);

        // Log the request
        auditLogService.logPermissionRequested(accountId, currentUser,
            request.getRequestedPermission(), permissionRequest.getCurrentPermission(), null);

        return ApiResponse.success("Permission request submitted successfully", permissionRequest);
    }

    public ApiResponse approvePermissionRequest(Long accountId, Long requestId,
                                              String reviewMessage, User currentUser) {
        // Validate ownership
        if (!permissionValidationService.canManagePermissions(currentUser, accountId)) {
            return ApiResponse.error("Access denied");
        }

        // Get and validate request
        PermissionRequest request = permissionRequestRepository.findById(requestId)
            .orElse(null);
        if (request == null || !request.getAccountId().equals(accountId)) {
            return ApiResponse.error("Permission request not found");
        }

        // Update request status
        request.setStatus(PermissionRequest.RequestStatus.APPROVED);
        request.setReviewedBy(currentUser.getId());
        request.setReviewMessage(reviewMessage);
        request.setReviewedAt(LocalDateTime.now());
        request = permissionRequestRepository.save(request);

        // Automatically update user's permission
        updateUserPermission(accountId, request.getRequesterId(),
                           request.getRequestedPermission(), currentUser);

        // Log approval
        User requestingUser = userRepository.findById(request.getRequesterId()).orElse(null);
        auditLogService.logPermissionRequestApproved(accountId, currentUser,
            requestingUser, request.getRequestedPermission(), null);

        return ApiResponse.success("Permission request approved successfully", request);
    }
}
```

## Frontend Integration

### React Permission Guards

The frontend uses permission guard components to conditionally render UI elements:

```javascript
// PermissionGuard.jsx
import React from 'react';

const PermissionGuard = ({
    account,
    requiredPermission,
    fallback = null,
    children
}) => {
    if (!account) return fallback;

    const hasPermission = checkPermission(account, requiredPermission);
    return hasPermission ? children : fallback;
};

// Permission check utility
const checkPermission = (account, requiredPermission) => {
    if (!account.userPermission) return false;

    const permissionLevels = {
        'VIEW_ONLY': 1,
        'TRANSACTION_ONLY': 2,
        'FULL_ACCESS': 3
    };

    const userLevel = permissionLevels[account.userPermission] || 0;
    const requiredLevel = permissionLevels[requiredPermission] || 0;

    return userLevel >= requiredLevel;
};

// Specialized guard components
export const OwnerOnlyGuard = ({ account, children, fallback }) => (
    <PermissionGuard
        account={account}
        requiredPermission="OWNER"
        fallback={fallback}
    >
        {account?.isOwner ? children : fallback}
    </PermissionGuard>
);

export const TransactionPermissionGuard = ({ account, children, fallback }) => (
    <PermissionGuard
        account={account}
        requiredPermission="TRANSACTION_ONLY"
        fallback={fallback}
    >
        {children}
    </PermissionGuard>
);

export const FullAccessGuard = ({ account, children, fallback }) => (
    <PermissionGuard
        account={account}
        requiredPermission="FULL_ACCESS"
        fallback={fallback}
    >
        {children}
    </PermissionGuard>
);

export default PermissionGuard;
```

### Usage in Components

```javascript
// AccountDashboard.jsx
import PermissionGuard, {
    OwnerOnlyGuard,
    TransactionPermissionGuard,
    FullAccessGuard
} from './PermissionGuard';

const AccountDashboard = ({ account }) => {
    return (
        <div className="account-dashboard">
            <h2>{account.name}</h2>
            <p>Balance: ${account.currentBalance}</p>

            {/* Everyone can see transactions */}
            <TransactionList account={account} />

            {/* Only transaction-level access can add transactions */}
            <TransactionPermissionGuard account={account}>
                <button onClick={handleAddTransaction}>
                    Add Transaction
                </button>
            </TransactionPermissionGuard>

            {/* Only full access can manage permissions */}
            <FullAccessGuard account={account}>
                <PermissionManagement account={account} />
            </FullAccessGuard>

            {/* Only owners can transfer ownership */}
            <OwnerOnlyGuard account={account}>
                <OwnershipControls account={account} />
            </OwnerOnlyGuard>
        </div>
    );
};
```

## Database Annotations

### Hibernate Type Handling

For PostgreSQL-specific types, use appropriate annotations:

```java
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Column(name = "action_details", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String actionDetails;

    @Column(name = "ip_address", columnDefinition = "inet")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.INET)
    private String ipAddress;

    // Lazy loading relationships should be JsonIgnored to prevent serialization issues
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
}
```

## Testing Strategy

### Integration Testing

Test the complete permission workflow:

```java
@SpringBootTest
@Transactional
class AccountPermissionIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testPermissionWorkflow() {
        // 1. Owner grants VIEW_ONLY to user
        ResponseEntity<ApiResponse> grantResponse = grantPermission(
            accountId, userId, "VIEW_ONLY");
        assertThat(grantResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. User requests upgrade to TRANSACTION_ONLY
        ResponseEntity<ApiResponse> requestResponse = createPermissionRequest(
            accountId, "TRANSACTION_ONLY", userToken);
        assertThat(requestResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. Owner approves request
        Long requestId = extractRequestId(requestResponse);
        ResponseEntity<ApiResponse> approveResponse = approveRequest(
            accountId, requestId, "Approved for transaction access");
        assertThat(approveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Verify permission was upgraded
        ResponseEntity<ApiResponse> permissionResponse = getAccountPermissions(accountId);
        assertThat(extractUserPermission(permissionResponse, userId))
            .isEqualTo("TRANSACTION_ONLY");

        // 5. Verify audit logs
        ResponseEntity<ApiResponse> auditResponse = getAuditLogs(accountId);
        assertThat(auditResponse.getBody().getData()).isNotNull();
        // Verify PERMISSION_GRANTED, PERMISSION_REQUESTED, PERMISSION_REQUEST_APPROVED logs
    }
}
```

### Unit Testing Security

Test permission validation logic:

```java
@ExtendWith(MockitoExtension.class)
class AccountSecurityEvaluatorTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AccountPermissionRepository permissionRepository;
    @InjectMocks private AccountSecurityEvaluator securityEvaluator;

    @Test
    void testOwnerHasAccess() {
        // Setup
        User owner = createUser(1L, "owner");
        Account account = createAccount(1L, owner);
        Authentication auth = createAuthentication(owner);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // Test
        boolean hasAccess = securityEvaluator.hasAccountAccess(auth, 1L);

        // Verify
        assertThat(hasAccess).isTrue();
    }

    @Test
    void testUserWithPermissionHasAccess() {
        // Setup
        User owner = createUser(1L, "owner");
        User user = createUser(2L, "user");
        Account account = createSharedAccount(1L, owner);
        Authentication auth = createAuthentication(user);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(permissionRepository.existsByAccountIdAndUserId(1L, 2L))
            .thenReturn(true);

        // Test
        boolean hasAccess = securityEvaluator.hasAccountAccess(auth, 1L);

        // Verify
        assertThat(hasAccess).isTrue();
    }
}
```

## Performance Considerations

### Database Optimizations

1. **Indexes for common queries:**
```sql
CREATE INDEX idx_account_permissions_lookup ON account_permissions(account_id, user_id);
CREATE INDEX idx_permission_requests_status ON permission_requests(account_id, status);
CREATE INDEX idx_audit_logs_account_date ON audit_logs(account_id, created_at DESC);
```

2. **Query optimization for audit logs:**
```java
// Use Spring Data method queries instead of native SQL
// to avoid pagination issues with custom queries
Page<AuditLog> findByAccountIdAndActionTypeOrderByCreatedAtDesc(
    Long accountId, AuditLog.ActionType actionType, Pageable pageable);
```

### Caching Strategy

Consider caching user permissions for frequently accessed accounts:

```java
@Service
public class PermissionCacheService {

    @Cacheable(value = "userPermissions", key = "#userId + '_' + #accountId")
    public Optional<PermissionType> getUserPermission(Long userId, Long accountId) {
        return accountPermissionRepository
            .findPermissionTypeByAccountIdAndUserId(accountId, userId);
    }

    @CacheEvict(value = "userPermissions", key = "#userId + '_' + #accountId")
    public void evictUserPermission(Long userId, Long accountId) {
        // Called when permissions change
    }
}
```

## Security Best Practices

### Input Validation
- Validate all permission type enums
- Sanitize user inputs for request messages
- Validate account ownership before any permission operations

### Authorization Layers
- Spring Security method-level authorization
- Service-layer permission validation
- Frontend permission guards
- Database constraints as final safety net

### Audit Trail
- Log all permission-related actions
- Capture IP addresses and user agents
- Store detailed action context in JSONB
- Implement log retention policies

### Error Handling
- Return generic error messages to prevent information disclosure
- Log detailed errors for debugging
- Implement proper exception handling at all layers

## Troubleshooting

### Common Issues

1. **"Could not initialize proxy" errors in JSON serialization:**
   - Add `@JsonIgnore` to lazy-loaded relationships
   - Use DTOs instead of entities for API responses

2. **Permission checks failing intermittently:**
   - Verify transaction boundaries in service methods
   - Check for stale authentication contexts
   - Ensure database constraints are properly defined

3. **Audit logs not saving:**
   - Check for JSON serialization errors in action details
   - Verify HTTP request context is available
   - Implement fallback logging for edge cases

4. **Frontend permission guards not updating:**
   - Ensure account data is refreshed after permission changes
   - Check for state management issues in React components
   - Verify API responses include updated permission information

This comprehensive system provides secure, auditable, and user-friendly account sharing capabilities while maintaining strict access controls and comprehensive logging.