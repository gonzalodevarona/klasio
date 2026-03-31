package com.klasio.student.domain.port;

import java.util.UUID;

public interface ActiveMembershipPort {

    boolean hasActiveMembership(UUID tenantId, UUID studentId);
}
