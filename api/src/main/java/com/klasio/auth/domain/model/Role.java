package com.klasio.auth.domain.model;

public enum Role {
    SUPERADMIN(0),
    ADMIN(1),
    MANAGER(2),
    PROFESSOR(3),
    STUDENT(4);

    private final int hierarchy;

    Role(int hierarchy) {
        this.hierarchy = hierarchy;
    }

    public boolean isAbove(Role other) {
        return this.hierarchy < other.hierarchy;
    }

    public String dashboardUrl() {
        return switch (this) {
            case SUPERADMIN -> "/superadmin/dashboard";
            case ADMIN -> "/admin/dashboard";
            case MANAGER -> "/manager/dashboard";
            case PROFESSOR -> "/professor/dashboard";
            case STUDENT -> "/student/dashboard";
        };
    }
}
