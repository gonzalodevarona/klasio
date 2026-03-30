package com.klasio.auth.domain.exception;

public class VerificationTokenExpiredException extends RuntimeException {
    public VerificationTokenExpiredException() {
        super("This verification link has expired. Please request a new one.");
    }
}
