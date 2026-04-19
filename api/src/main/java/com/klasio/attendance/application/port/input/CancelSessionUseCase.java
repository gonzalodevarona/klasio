package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.CancelSessionCommand;
import com.klasio.attendance.application.dto.SessionCancellationResult;

public interface CancelSessionUseCase {
    SessionCancellationResult execute(CancelSessionCommand command);
}
