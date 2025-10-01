package com.checkbook.api.dto;

import com.checkbook.api.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateAccountRequest {

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must be less than 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @NotNull(message = "Account type is required")
    private AccountType accountType;

    @Size(max = 100, message = "Bank name must be less than 100 characters")
    private String bankName;

    @Size(max = 20, message = "Account number mask must be less than 20 characters")
    private String accountNumberMasked;

    @NotNull(message = "Shared status is required")
    private Boolean isShared;

    public UpdateAccountRequest() {}

    public UpdateAccountRequest(String name, AccountType accountType, Boolean isShared) {
        this.name = name;
        this.accountType = accountType;
        this.isShared = isShared;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumberMasked() {
        return accountNumberMasked;
    }

    public void setAccountNumberMasked(String accountNumberMasked) {
        this.accountNumberMasked = accountNumberMasked;
    }

    public Boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }
}