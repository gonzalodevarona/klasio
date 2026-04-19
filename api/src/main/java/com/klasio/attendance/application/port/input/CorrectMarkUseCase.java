package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.CorrectMarkCommand;
import com.klasio.attendance.application.dto.MarkAttendanceResult.MarkedRegistration;

public interface CorrectMarkUseCase {
    MarkedRegistration execute(CorrectMarkCommand command);
}
