package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.MembershipHoursPort;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.infrastructure.persistence.SpringDataMembershipRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class MembershipHoursAdapter implements MembershipHoursPort {

    private final SpringDataMembershipRepository membershipRepository;

    public MembershipHoursAdapter(SpringDataMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Optional<ActiveMembershipView> findActiveForStudentInProgram(UUID tenantId, UUID studentId,
                                                                         UUID programId) {
        return membershipRepository
                .findByTenantIdAndStudentIdAndProgramIdAndStatus(
                        tenantId, studentId, programId, MembershipStatus.ACTIVE.name())
                .map(e -> new ActiveMembershipView(e.getId(), e.getAvailableHours(), e.getExpirationDate()));
    }
}
