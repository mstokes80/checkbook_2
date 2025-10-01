package com.checkbook.api.service;

import com.checkbook.api.dto.ApiResponse;
import com.checkbook.api.dto.PermissionRequestDto;
import com.checkbook.api.entity.*;
import com.checkbook.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PermissionRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionRequestService.class);

    @Autowired
    private PermissionRequestRepository permissionRequestRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountPermissionRepository accountPermissionRepository;

    @Autowired
    private AuditLogService auditLogService;

    @PreAuthorize("hasRole('USER')")
    public ApiResponse createPermissionRequest(Long accountId, PermissionRequestDto request, User requestingUser) {
        try {
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found");
            }

            Account account = accountOpt.get();

            if (!account.getIsShared()) {
                return ApiResponse.error("Cannot request permissions for non-shared accounts");
            }

            if (account.isOwner(requestingUser)) {
                return ApiResponse.error("Account owners cannot request permissions for their own accounts");
            }

            if (permissionRequestRepository.existsByAccountIdAndRequesterIdAndStatus(
                    accountId, requestingUser.getId(), PermissionRequest.RequestStatus.PENDING)) {
                return ApiResponse.error("You already have a pending permission request for this account");
            }

            PermissionType currentPermission = getCurrentPermission(accountId, requestingUser.getId());

            if (currentPermission != null && currentPermission.includes(request.getRequestedPermission())) {
                return ApiResponse.error("You already have this permission level or higher");
            }

            PermissionRequest permissionRequest = new PermissionRequest(
                    accountId,
                    requestingUser.getId(),
                    request.getRequestedPermission(),
                    currentPermission,
                    request.getRequestMessage()
            );

            PermissionRequest savedRequest = permissionRequestRepository.save(permissionRequest);

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("requestMessage", request.getRequestMessage());
            auditDetails.put("requestId", savedRequest.getId());
            auditLogService.logPermissionRequested(
                    accountId,
                    requestingUser,
                    request.getRequestedPermission().name(),
                    currentPermission != null ? currentPermission.name() : null,
                    auditDetails
            );

            logger.info("Permission request created: User '{}' requested '{}' for account {}",
                       requestingUser.getUsername(), request.getRequestedPermission(), accountId);

            return ApiResponse.success("Permission request submitted successfully", convertToDto(savedRequest));

        } catch (Exception e) {
            logger.error("Error creating permission request for account {} by user '{}': {}",
                        accountId, requestingUser.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to create permission request: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER') and @accountSecurity.isAccountOwner(authentication, #accountId)")
    public ApiResponse approvePermissionRequest(Long accountId, Long requestId, String reviewMessage, User reviewingUser) {
        try {
            Optional<PermissionRequest> requestOpt = permissionRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                return ApiResponse.error("Permission request not found");
            }

            PermissionRequest permissionRequest = requestOpt.get();

            if (!permissionRequest.getAccountId().equals(accountId)) {
                return ApiResponse.error("Permission request does not belong to this account");
            }

            if (!permissionRequest.isPending()) {
                return ApiResponse.error("Permission request is not pending");
            }

            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, reviewingUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("You can only approve requests for accounts you own");
            }

            permissionRequest.approve(reviewingUser.getId(), reviewMessage);
            permissionRequestRepository.save(permissionRequest);

            Optional<User> requesterOpt = userRepository.findById(permissionRequest.getRequesterId());
            if (!requesterOpt.isPresent()) {
                return ApiResponse.error("Requesting user not found");
            }

            User requestingUser = requesterOpt.get();
            Account account = accountOpt.get();

            Optional<AccountPermission> existingPermission = accountPermissionRepository
                    .findByAccountIdAndUserId(accountId, requestingUser.getId());

            if (existingPermission.isPresent()) {
                AccountPermission permission = existingPermission.get();
                PermissionType oldPermission = permission.getPermissionType();
                permission.setPermissionType(permissionRequest.getRequestedPermission());
                accountPermissionRepository.save(permission);

                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("requestId", requestId);
                auditDetails.put("reviewMessage", reviewMessage);
                auditLogService.logPermissionModified(accountId, reviewingUser, requestingUser,
                                                     oldPermission.name(), permissionRequest.getRequestedPermission().name(), auditDetails);
            } else {
                AccountPermission newPermission = new AccountPermission(account, requestingUser, permissionRequest.getRequestedPermission());
                accountPermissionRepository.save(newPermission);

                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("requestId", requestId);
                auditDetails.put("reviewMessage", reviewMessage);
                auditLogService.logPermissionGranted(accountId, reviewingUser, requestingUser,
                                                    permissionRequest.getRequestedPermission().name(), auditDetails);
            }

            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("requestId", requestId);
            auditDetails.put("reviewMessage", reviewMessage);
            auditLogService.logPermissionRequestApproved(accountId, reviewingUser, requestingUser,
                                                        permissionRequest.getRequestedPermission().name(), auditDetails);

            logger.info("Permission request approved: Request {} for user '{}' on account {} by user '{}'",
                       requestId, requestingUser.getUsername(), accountId, reviewingUser.getUsername());

            return ApiResponse.success("Permission request approved successfully", convertToDto(permissionRequest));

        } catch (Exception e) {
            logger.error("Error approving permission request {} for account {}: {}", requestId, accountId, e.getMessage());
            return ApiResponse.error("Failed to approve permission request: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER') and @accountSecurity.isAccountOwner(authentication, #accountId)")
    public ApiResponse denyPermissionRequest(Long accountId, Long requestId, String reviewMessage, User reviewingUser) {
        try {
            Optional<PermissionRequest> requestOpt = permissionRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                return ApiResponse.error("Permission request not found");
            }

            PermissionRequest permissionRequest = requestOpt.get();

            if (!permissionRequest.getAccountId().equals(accountId)) {
                return ApiResponse.error("Permission request does not belong to this account");
            }

            if (!permissionRequest.isPending()) {
                return ApiResponse.error("Permission request is not pending");
            }

            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, reviewingUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("You can only deny requests for accounts you own");
            }

            permissionRequest.deny(reviewingUser.getId(), reviewMessage);
            permissionRequestRepository.save(permissionRequest);

            Optional<User> requesterOpt = userRepository.findById(permissionRequest.getRequesterId());
            if (requesterOpt.isPresent()) {
                User requestingUser = requesterOpt.get();

                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("requestId", requestId);
                auditDetails.put("reviewMessage", reviewMessage);
                auditLogService.logPermissionRequestDenied(accountId, reviewingUser, requestingUser,
                                                          permissionRequest.getRequestedPermission().name(), reviewMessage, auditDetails);
            }

            logger.info("Permission request denied: Request {} for account {} by user '{}'",
                       requestId, accountId, reviewingUser.getUsername());

            return ApiResponse.success("Permission request denied successfully", convertToDto(permissionRequest));

        } catch (Exception e) {
            logger.error("Error denying permission request {} for account {}: {}", requestId, accountId, e.getMessage());
            return ApiResponse.error("Failed to deny permission request: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER')")
    public ApiResponse cancelPermissionRequest(Long requestId, User requestingUser) {
        try {
            Optional<PermissionRequest> requestOpt = permissionRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                return ApiResponse.error("Permission request not found");
            }

            PermissionRequest permissionRequest = requestOpt.get();

            if (!permissionRequest.getRequesterId().equals(requestingUser.getId())) {
                return ApiResponse.error("You can only cancel your own permission requests");
            }

            if (!permissionRequest.isPending()) {
                return ApiResponse.error("Permission request is not pending");
            }

            permissionRequest.cancel();
            permissionRequestRepository.save(permissionRequest);

            logger.info("Permission request cancelled: Request {} by user '{}'",
                       requestId, requestingUser.getUsername());

            return ApiResponse.success("Permission request cancelled successfully", convertToDto(permissionRequest));

        } catch (Exception e) {
            logger.error("Error cancelling permission request {}: {}", requestId, e.getMessage());
            return ApiResponse.error("Failed to cancel permission request: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER') and @accountSecurity.isAccountOwner(authentication, #accountId)")
    public ApiResponse getAccountPermissionRequests(Long accountId, User accountOwner, Pageable pageable) {
        try {
            Page<PermissionRequest> requests = permissionRequestRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
            Page<PermissionRequestDto> requestDtos = requests.map(this::convertToDto);

            return ApiResponse.success("Permission requests retrieved successfully", requestDtos);

        } catch (Exception e) {
            logger.error("Error retrieving permission requests for account {}: {}", accountId, e.getMessage());
            return ApiResponse.error("Failed to retrieve permission requests: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER')")
    public ApiResponse getUserPermissionRequests(User user, Pageable pageable) {
        try {
            Page<PermissionRequest> requests = permissionRequestRepository.findByRequesterIdOrderByCreatedAtDesc(user.getId(), pageable);
            Page<PermissionRequestDto> requestDtos = requests.map(this::convertToDto);

            return ApiResponse.success("Your permission requests retrieved successfully", requestDtos);

        } catch (Exception e) {
            logger.error("Error retrieving permission requests for user '{}': {}", user.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to retrieve permission requests: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER')")
    public ApiResponse getAccountOwnerPermissionRequests(User accountOwner, Pageable pageable) {
        try {
            Page<PermissionRequest> requests = permissionRequestRepository.findByAccountOwnerIdOrderByCreatedAtDesc(accountOwner.getId(), pageable);
            Page<PermissionRequestDto> requestDtos = requests.map(this::convertToDto);

            return ApiResponse.success("Permission requests for your accounts retrieved successfully", requestDtos);

        } catch (Exception e) {
            logger.error("Error retrieving permission requests for account owner '{}': {}", accountOwner.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to retrieve permission requests: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('USER')")
    public ApiResponse getPendingPermissionRequests(User accountOwner) {
        try {
            List<PermissionRequest> pendingRequests = permissionRequestRepository
                    .findByAccountOwnerIdAndStatusOrderByCreatedAtDesc(accountOwner.getId(), PermissionRequest.RequestStatus.PENDING);

            List<PermissionRequestDto> requestDtos = pendingRequests.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            return ApiResponse.success("Pending permission requests retrieved successfully", requestDtos);

        } catch (Exception e) {
            logger.error("Error retrieving pending permission requests for user '{}': {}", accountOwner.getUsername(), e.getMessage());
            return ApiResponse.error("Failed to retrieve pending permission requests: " + e.getMessage());
        }
    }

    public long getPendingRequestCount(User accountOwner) {
        return permissionRequestRepository.countByAccountOwnerIdAndStatus(accountOwner.getId(), PermissionRequest.RequestStatus.PENDING);
    }

    @PreAuthorize("hasRole('USER')")
    public ApiResponse getPermissionRequestWithFilters(Long accountId, PermissionRequest.RequestStatus status,
                                                      Long requesterId, LocalDateTime startDate, LocalDateTime endDate,
                                                      User currentUser, Pageable pageable) {
        try {
            Optional<Account> accountOpt = accountRepository.findByIdAndOwnerId(accountId, currentUser.getId());
            if (!accountOpt.isPresent()) {
                return ApiResponse.error("Account not found or you don't own this account");
            }

            Page<PermissionRequest> requests = permissionRequestRepository.findWithFilters(
                    accountId, status, requesterId, startDate, endDate, pageable);

            Page<PermissionRequestDto> requestDtos = requests.map(this::convertToDto);

            return ApiResponse.success("Filtered permission requests retrieved successfully", requestDtos);

        } catch (Exception e) {
            logger.error("Error retrieving filtered permission requests for account {}: {}", accountId, e.getMessage());
            return ApiResponse.error("Failed to retrieve filtered permission requests: " + e.getMessage());
        }
    }

    public void cleanupOldProcessedRequests(LocalDateTime cutoffDate) {
        logger.info("Cleaning up old processed permission requests older than {}", cutoffDate);
        permissionRequestRepository.deleteByStatusNotAndCreatedAtBefore(PermissionRequest.RequestStatus.PENDING, cutoffDate);
        logger.info("Completed cleanup of old processed permission requests");
    }

    private PermissionType getCurrentPermission(Long accountId, Long userId) {
        Optional<PermissionType> permission = accountPermissionRepository.findPermissionTypeByAccountIdAndUserId(accountId, userId);
        return permission.orElse(null);
    }

    private PermissionRequestDto convertToDto(PermissionRequest request) {
        PermissionRequestDto dto = new PermissionRequestDto();
        dto.setId(request.getId());
        dto.setAccountId(request.getAccountId());
        dto.setRequesterId(request.getRequesterId());
        dto.setRequestedPermission(request.getRequestedPermission());
        dto.setCurrentPermission(request.getCurrentPermission());
        dto.setRequestMessage(request.getRequestMessage());
        dto.setStatus(request.getStatus());
        dto.setReviewedBy(request.getReviewedBy());
        dto.setReviewMessage(request.getReviewMessage());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setReviewedAt(request.getReviewedAt());

        if (request.getAccount() != null) {
            dto.setAccountName(request.getAccount().getName());
        }
        if (request.getRequester() != null) {
            dto.setRequesterUsername(request.getRequester().getUsername());
            dto.setRequesterEmail(request.getRequester().getEmail());
            dto.setRequesterFullName(request.getRequester().getFirstName() + " " + request.getRequester().getLastName());
        }
        if (request.getReviewer() != null) {
            dto.setReviewerUsername(request.getReviewer().getUsername());
            dto.setReviewerFullName(request.getReviewer().getFirstName() + " " + request.getReviewer().getLastName());
        }

        return dto;
    }
}