package com.klasio.notifications.application.port.input;

import java.util.UUID;

public interface GetUnreadCountUseCase {
    record Result(long count, boolean hasCancellation) {}

    Result execute(UUID tenantId, UUID userId);
}
