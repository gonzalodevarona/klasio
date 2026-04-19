package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.AvailableSessionView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface GetAvailableSessionsUseCase {
    List<AvailableSessionView> execute(UUID tenantId, UUID studentId, UUID programId,
                                       LocalDate from, LocalDate to, boolean includeFull);
}
