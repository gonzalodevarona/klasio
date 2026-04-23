package com.klasio.auth.domain.exception;

public class AccountSetupTokenExpiredException extends RuntimeException {
    public AccountSetupTokenExpiredException() {
        super("This setup link has expired. Please request a new one.");
    }
}
