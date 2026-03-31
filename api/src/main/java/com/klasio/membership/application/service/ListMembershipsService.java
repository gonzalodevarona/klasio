package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.ListMembershipsUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListMembershipsService implements ListMembershipsUseCase {

    private final MembershipRepository membershipRepository;

    public ListMembershipsService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public Page<Membership> execute(UUID tenantId, UUID studentId, UUID programId,
                                    MembershipStatus status, int page, int size) {
        return membershipRepository.findAll(tenantId, studentId, programId, status, page, size);
    }
}
