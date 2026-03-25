package com.klasio.shared.infrastructure.exception;

public class ProfessorAlreadyActiveException extends RuntimeException {

    public ProfessorAlreadyActiveException(String message) {
        super(message);
    }
}
