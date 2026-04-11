package com.klasio.membership.application.port.input;

import java.util.List;
import java.util.UUID;

public interface ListDelegatedMembershipsUseCase {
    List<DelegatedMembershipDto> execute(UUID tenantId, UUID programId);
}
