package com.checkbook.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name must be less than 100 characters")
    private String name;

    @Column(columnDefinition = "TEXT")
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @NotNull(message = "Account type is required")
    private AccountType accountType = AccountType.CHECKING;

    @Column(name = "bank_name", length = 100)
    @Size(max = 100, message = "Bank name must be less than 100 characters")
    private String bankName;

    @Column(name = "account_number_masked", length = 20)
    @Size(max = 20, message = "Account number mask must be less than 20 characters")
    private String accountNumberMasked;

    @Column(name = "is_shared", nullable = false)
    @NotNull(message = "Shared status is required")
    private Boolean isShared = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @NotNull(message = "Account owner is required")
    private User owner;

    @Column(name = "current_balance", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Current balance is required")
    private BigDecimal currentBalance = BigDecimal.ZERO;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    // Future relationship for transactions
    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Transaction> transactions = new ArrayList<>();

    // Future relationship for account permissions (for shared accounts)
    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<AccountPermission> permissions = new ArrayList<>();

    // Constructors
    public Account() {}

    public Account(String name, AccountType accountType, User owner, Boolean isShared) {
        this.name = name;
        this.accountType = accountType;
        this.owner = owner;
        this.isShared = isShared;
        this.currentBalance = BigDecimal.ZERO;
    }

    // Business methods
    public void updateBalance(BigDecimal newBalance) {
        this.currentBalance = newBalance;
    }

    public boolean isOwner(User user) {
        return this.owner != null && this.owner.getId().equals(user.getId());
    }

    public String getDisplayName() {
        if (bankName != null && !bankName.trim().isEmpty()) {
            return name + " (" + bankName + ")";
        }
        return name;
    }

    public String getMaskedAccountInfo() {
        if (accountNumberMasked != null && !accountNumberMasked.trim().isEmpty()) {
            return getDisplayName() + " " + accountNumberMasked;
        }
        return getDisplayName();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                ", isShared=" + isShared +
                ", currentBalance=" + currentBalance +
                '}';
    }
}