package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;

public interface RegisterWalkInBulkUseCase {
    WalkInBulkResult execute(RegisterWalkInBulkCommand command);
}
