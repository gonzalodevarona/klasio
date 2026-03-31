package com.klasio.shared.infrastructure.exception;

public class MembershipAlreadyActiveException extends RuntimeException {
    public MembershipAlreadyActiveException(String message) { super(message); }
}
