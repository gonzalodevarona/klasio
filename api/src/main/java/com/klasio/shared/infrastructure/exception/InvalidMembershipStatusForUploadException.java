package com.klasio.shared.infrastructure.exception;

public class InvalidMembershipStatusForUploadException extends RuntimeException {
    public InvalidMembershipStatusForUploadException(String message) {
        super(message);
    }
}
