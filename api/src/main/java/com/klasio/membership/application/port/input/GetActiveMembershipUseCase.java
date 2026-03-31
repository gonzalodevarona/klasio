package com.klasio.membership.application.port.input;
import com.klasio.membership.domain.model.Membership;
import java.util.UUID;
public interface GetActiveMembershipUseCase {
    Membership execute(UUID tenantId, UUID studentId, UUID programId);
}
