package com.klasio.membership.domain.model;

import java.util.Objects;
import java.util.UUID;

public record PaymentProofId(UUID value) {
    public PaymentProofId {
        Objects.requireNonNull(value, "PaymentProof id must not be null");
    }
    public static PaymentProofId generate() { return new PaymentProofId(UUID.randomUUID()); }
    public static PaymentProofId of(UUID id) { return new PaymentProofId(id); }
}
