package com.klasio.membership.infrastructure.persistence;

import com.klasio.student.domain.port.ActiveMembershipPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActiveMembershipAdapter implements ActiveMembershipPort {

    private final JpaMembershipRepository membershipRepository;

    public ActiveMembershipAdapter(JpaMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public boolean hasActiveMembership(UUID tenantId, UUID studentId) {
        return membershipRepository.existsActiveMembershipForStudent(tenantId, studentId);
    }
}
