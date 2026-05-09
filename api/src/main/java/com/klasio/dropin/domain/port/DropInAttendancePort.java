package com.klasio.dropin.domain.port;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface DropInAttendancePort {

    UUID recordPresent(RecordDropInPresentCommand cmd);

    record RecordDropInPresentCommand(
            UUID tenantId,
            UUID sessionId,
            UUID classId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            int maxCapacity,
            UUID attendeeId,
            UUID paymentId,
            UUID actorUserId,
            Instant now
    ) {}
}
