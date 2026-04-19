package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.MarkAttendanceCommand;
import com.klasio.attendance.application.dto.MarkAttendanceResult;

public interface MarkAttendanceUseCase {
    MarkAttendanceResult execute(MarkAttendanceCommand command);
}
