package com.klasio.auth.domain.exception;

public class AccountSetupPendingException extends RuntimeException {
    public AccountSetupPendingException() {
        super("Account setup is pending. Please check your email for the setup link.");
    }
}
