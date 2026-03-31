package com.klasio.auth.domain.exception;

public class ResetTokenExpiredException extends RuntimeException {
    public ResetTokenExpiredException() {
        super("This reset link has expired. Please request a new one.");
    }
}
