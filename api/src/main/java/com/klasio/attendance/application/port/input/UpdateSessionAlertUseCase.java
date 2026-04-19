package com.klasio.attendance.application.port.input;

import com.klasio.attendance.application.dto.SessionActionResult;
import com.klasio.attendance.application.dto.UpdateSessionAlertCommand;

public interface UpdateSessionAlertUseCase {
    SessionActionResult execute(UpdateSessionAlertCommand command);
}
