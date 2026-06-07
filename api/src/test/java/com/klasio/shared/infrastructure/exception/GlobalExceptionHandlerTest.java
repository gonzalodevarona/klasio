package com.klasio.shared.infrastructure.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handlesStudentIdentityNumberConflict_as409() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleStudentIdentityNumberConflict(
                new StudentIdentityNumberAlreadyExistsException("dup"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error().code()).isEqualTo("STUDENT_IDENTITY_NUMBER_EXISTS");
        assertThat(response.getBody().error().message()).isEqualTo("dup");
    }
}
