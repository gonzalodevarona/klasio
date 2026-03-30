package com.klasio.auth.domain.exception;

public class ResetTokenAlreadyUsedException extends RuntimeException {
    public ResetTokenAlreadyUsedException() {
        super("This reset link has already been used.");
    }
}
