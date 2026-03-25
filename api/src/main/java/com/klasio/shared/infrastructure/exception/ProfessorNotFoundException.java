package com.klasio.shared.infrastructure.exception;

public class ProfessorNotFoundException extends RuntimeException {

    public ProfessorNotFoundException(String message) {
        super(message);
    }
}
