package com.klasio.dropin.domain.port;

import com.klasio.dropin.domain.model.DropInPayment;

import java.util.Optional;
import java.util.UUID;

public interface DropInPaymentRepository {

    DropInPayment save(DropInPayment payment);

    Optional<DropInPayment> findByAttendeeAndSession(UUID attendeeId, UUID sessionId);
}
