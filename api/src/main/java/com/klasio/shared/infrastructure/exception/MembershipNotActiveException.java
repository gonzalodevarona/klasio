package com.klasio.shared.infrastructure.exception;

public class MembershipNotActiveException extends RuntimeException {
    public MembershipNotActiveException(String message) { super(message); }
}
