package com.klasio.auth.domain.model;

import java.util.Set;

public enum Role {
    SUPERADMIN(0),
    ADMIN(1),
    MANAGER(2),
    PROFESSOR(3),
    STUDENT(4);

    public final int hierarchy;

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

    /**
     * Returns the set of roles implied by this role.
     * A MANAGER is also a PROFESSOR — when a MANAGER is created or assigned,
     * the PROFESSOR role is automatically included.
     */
    public Set<Role> impliedRoles() {
        return switch (this) {
            case MANAGER -> Set.of(MANAGER, PROFESSOR);
            default      -> Set.of(this);
        };
    }
}
