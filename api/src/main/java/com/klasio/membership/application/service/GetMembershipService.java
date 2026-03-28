package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.GetMembershipUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetMembershipService implements GetMembershipUseCase {

    private final MembershipRepository membershipRepository;

    public GetMembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public Membership execute(UUID tenantId, UUID membershipId) {
        return membershipRepository.findById(tenantId, membershipId)
                .orElseThrow(() -> new MembershipNotFoundException(
                        "Membership not found: " + membershipId));
    }
}
