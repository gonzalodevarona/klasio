package com.klasio.attendance.infrastructure.web;

import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.SessionCancellationResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class SessionLifecycleDtos {

    private SessionLifecycleDtos() {}

    public record ReasonBody(
            @NotBlank @Size(min = 20, max = 500) String reason) {}

    public record SessionActionResponse(
            UUID sessionId, String status, String reason, UUID actorId, Instant timestamp) {

        public static SessionActionResponse from(SessionActionResult r) {
            return new SessionActionResponse(r.sessionId(), r.status(), r.reason(), r.actorId(), r.timestamp());
        }
    }

    public record SessionCancellationResponse(
            UUID sessionId, String status, String reason, UUID actorId, Instant timestamp,
            int affectedStudentCount) {

        public static SessionCancellationResponse from(SessionCancellationResult r) {
            return new SessionCancellationResponse(r.sessionId(), r.status(), r.reason(), r.actorId(),
                    r.timestamp(), r.affectedStudentCount());
        }
    }
}
