package com.klasio.shared.infrastructure.exception;

public class MembershipNotFoundException extends RuntimeException {
    public MembershipNotFoundException(String message) { super(message); }
}
