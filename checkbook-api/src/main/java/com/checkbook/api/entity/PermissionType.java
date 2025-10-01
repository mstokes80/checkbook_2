package com.checkbook.api.entity;

public enum PermissionType {
    VIEW_ONLY("View Only", 1),
    TRANSACTION_ONLY("Transaction Only", 2),
    FULL_ACCESS("Full Access", 3);

    private final String displayName;
    private final int level;

    PermissionType(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Check if this permission includes the capabilities of another permission
     * @param other the permission to check against
     * @return true if this permission includes the other's capabilities
     */
    public boolean includes(PermissionType other) {
        return this.level >= other.level;
    }

    /**
     * Check if this permission allows viewing account details
     * @return true if viewing is allowed
     */
    public boolean canView() {
        return this.level >= VIEW_ONLY.level;
    }

    /**
     * Check if this permission allows adding transactions
     * @return true if transaction management is allowed
     */
    public boolean canManageTransactions() {
        return this.level >= TRANSACTION_ONLY.level;
    }

    /**
     * Check if this permission allows full account modifications
     * @return true if full access is allowed
     */
    public boolean canModifyAccount() {
        return this.level >= FULL_ACCESS.level;
    }
}