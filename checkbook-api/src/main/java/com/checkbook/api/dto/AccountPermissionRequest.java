package com.checkbook.api.dto;

import com.checkbook.api.entity.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AccountPermissionRequest {

    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotNull(message = "Permission type is required")
    private PermissionType permissionType;

    public AccountPermissionRequest() {}

    public AccountPermissionRequest(String usernameOrEmail, PermissionType permissionType) {
        this.usernameOrEmail = usernameOrEmail;
        this.permissionType = permissionType;
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public PermissionType getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(PermissionType permissionType) {
        this.permissionType = permissionType;
    }
}