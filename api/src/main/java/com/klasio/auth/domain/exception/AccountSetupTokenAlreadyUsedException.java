package com.klasio.auth.domain.exception;

public class AccountSetupTokenAlreadyUsedException extends RuntimeException {
    public AccountSetupTokenAlreadyUsedException() {
        super("This setup link has already been used.");
    }
}
