package com.klasio.notifications.application.port.input;

import com.klasio.notifications.application.dto.CreateNotificationCommand;
import java.util.UUID;

public interface CreateNotificationUseCase {
    UUID execute(CreateNotificationCommand command);
}
