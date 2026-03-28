package com.klasio.membership.domain.model;

import java.util.Objects;
import java.util.UUID;

public record MembershipId(UUID value) {
    public MembershipId {
        Objects.requireNonNull(value, "Membership id must not be null");
    }
    public static MembershipId generate() { return new MembershipId(UUID.randomUUID()); }
    public static MembershipId of(UUID id) { return new MembershipId(id); }
}
