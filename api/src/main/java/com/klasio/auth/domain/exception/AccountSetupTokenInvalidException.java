package com.klasio.auth.domain.exception;

public class AccountSetupTokenInvalidException extends RuntimeException {
    public AccountSetupTokenInvalidException() {
        super("This setup link is invalid. Please request a new one.");
    }
}
