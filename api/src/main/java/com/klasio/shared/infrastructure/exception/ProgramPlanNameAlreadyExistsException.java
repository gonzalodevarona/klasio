package com.klasio.shared.infrastructure.exception;

public class ProgramPlanNameAlreadyExistsException extends RuntimeException {

    public ProgramPlanNameAlreadyExistsException(String message) {
        super(message);
    }
}
