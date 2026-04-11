package com.klasio.shared.infrastructure.exception;

public class PaymentProofNotFoundException extends RuntimeException {
    public PaymentProofNotFoundException(String message) {
        super(message);
    }
}
