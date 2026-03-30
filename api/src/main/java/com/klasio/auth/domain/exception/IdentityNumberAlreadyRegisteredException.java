package com.klasio.auth.domain.exception;

public class IdentityNumberAlreadyRegisteredException extends RuntimeException {
    public IdentityNumberAlreadyRegisteredException() {
        super("This identity document number is already registered in this league");
    }
}
