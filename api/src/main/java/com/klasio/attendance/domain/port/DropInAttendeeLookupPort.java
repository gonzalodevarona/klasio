package com.klasio.attendance.domain.port;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface DropInAttendeeLookupPort {
    Map<UUID, DropInRosterEntry> findByAttendeeIds(UUID tenantId, Set<UUID> attendeeIds);
    Map<UUID, BigDecimal> findAmountsByPaymentIds(UUID tenantId, Set<UUID> paymentIds);

    record DropInRosterEntry(String fullName, String phone) {}
}
