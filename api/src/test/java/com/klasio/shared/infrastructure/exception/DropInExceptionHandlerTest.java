package com.klasio.shared.infrastructure.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DropInExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/test/drop-in-not-available")
        void throwNotAvailable() { throw new DropInNotAvailableException("Drop-in not enabled for this program"); }

        @GetMapping("/test/drop-in-attendee-not-found")
        void throwNotFound() { throw new DropInAttendeeNotFoundException("Attendee not found"); }

        @GetMapping("/test/phone-exists")
        void throwPhoneExists() {
            throw new PhoneAlreadyExistsException(UUID.randomUUID(), "Ana García", 3);
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void dropInNotAvailable_returns422() throws Exception {
        mockMvc.perform(get("/test/drop-in-not-available"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("DROP_IN_NOT_AVAILABLE"))
            .andExpect(jsonPath("$.error.message").value("Drop-in not enabled for this program"));
    }

    @Test
    void dropInAttendeeNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test/drop-in-attendee-not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("DROP_IN_ATTENDEE_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").value("Attendee not found"));
    }

    @Test
    void phoneAlreadyExists_returns409WithAttendeeInfo() throws Exception {
        mockMvc.perform(get("/test/phone-exists"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("DROP_IN_PHONE_EXISTS"))
            .andExpect(jsonPath("$.existingAttendeeId").isNotEmpty())
            .andExpect(jsonPath("$.fullName").value("Ana García"))
            .andExpect(jsonPath("$.totalVisits").value(3));
    }
}
