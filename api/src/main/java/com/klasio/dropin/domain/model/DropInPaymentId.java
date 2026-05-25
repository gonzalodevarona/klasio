package com.klasio.dropin.domain.model;

import java.util.Objects;
import java.util.UUID;

public record DropInPaymentId(UUID value) {

    public DropInPaymentId {
        Objects.requireNonNull(value, "DropInPaymentId value must not be null");
    }

    public static DropInPaymentId generate() {
        return new DropInPaymentId(UUID.randomUUID());
    }

    public static DropInPaymentId of(UUID value) {
        return new DropInPaymentId(value);
    }
}
