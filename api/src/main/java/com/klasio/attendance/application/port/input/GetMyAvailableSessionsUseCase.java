package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.AvailableSessionView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Aggregator use case: returns the student's available sessions across all enrolled programs
 * in a single call. Centralises the fan-out so the frontend issues exactly one HTTP request.
 */
public interface GetMyAvailableSessionsUseCase {
    List<AvailableSessionView> execute(UUID tenantId, UUID studentId,
                                       LocalDate from, LocalDate to, boolean includeFull);
}
