package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.GetActiveMembershipUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetActiveMembershipService implements GetActiveMembershipUseCase {

    private final MembershipRepository membershipRepository;

    public GetActiveMembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public Membership execute(UUID tenantId, UUID studentId, UUID programId) {
        return membershipRepository.findActiveByStudentIdAndProgramId(tenantId, studentId, programId)
                .orElseThrow(() -> new MembershipNotFoundException(
                        "No active membership found for student %s in program %s"
                                .formatted(studentId, programId)));
    }
}
