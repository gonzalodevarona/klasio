package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.RaiseSessionAlertCommand;
import com.klasio.attendance.application.dto.SessionActionResult;

public interface RaiseSessionAlertUseCase {
    SessionActionResult execute(RaiseSessionAlertCommand command);
}
