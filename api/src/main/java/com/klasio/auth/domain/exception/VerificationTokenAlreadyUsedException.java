package com.klasio.auth.domain.exception;

public class VerificationTokenAlreadyUsedException extends RuntimeException {
    public VerificationTokenAlreadyUsedException() {
        super("This verification link has already been used.");
    }
}
