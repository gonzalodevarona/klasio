package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.AttendanceStatsView;
import java.util.UUID;

public interface GetAttendanceStatsUseCase {
    AttendanceStatsView execute(UUID tenantId, UUID studentId);
}
