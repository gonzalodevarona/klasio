package com.klasio.dropin.domain.port;

import com.klasio.dropin.domain.model.DropInAttendee;

import java.util.Optional;
import java.util.UUID;

public interface DropInAttendeeRepository {

    DropInAttendee save(DropInAttendee attendee);

    Optional<DropInAttendee> findByIdAndTenant(UUID id, UUID tenantId);

    Optional<DropInAttendee> findByPhoneAndTenant(String phone, UUID tenantId);
}
