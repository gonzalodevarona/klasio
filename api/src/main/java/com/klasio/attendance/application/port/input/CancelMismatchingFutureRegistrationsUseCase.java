package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.CancelMismatchingFutureRegistrationsCommand;

public interface CancelMismatchingFutureRegistrationsUseCase {
    int execute(CancelMismatchingFutureRegistrationsCommand command);
}
