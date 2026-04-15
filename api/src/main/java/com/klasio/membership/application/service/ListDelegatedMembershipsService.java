package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.DelegatedMembershipDto;
import com.klasio.membership.application.port.input.ListDelegatedMembershipsUseCase;
import com.klasio.membership.domain.model.DelegationReminder;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.membership.infrastructure.persistence.DelegationReminderJpaAdapter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListDelegatedMembershipsService implements ListDelegatedMembershipsUseCase {

    private final MembershipRepository membershipRepository;
    private final DelegationReminderJpaAdapter delegationReminderAdapter;
    private final PaymentProofRepository proofRepository;
    private final StudentNamePort studentNamePort;
    private final ProgramNamePort programNamePort;

    public ListDelegatedMembershipsService(MembershipRepository membershipRepository,
                                           DelegationReminderJpaAdapter delegationReminderAdapter,
                                           PaymentProofRepository proofRepository,
                                           StudentNamePort studentNamePort,
                                           ProgramNamePort programNamePort) {
        this.membershipRepository = membershipRepository;
        this.delegationReminderAdapter = delegationReminderAdapter;
        this.proofRepository = proofRepository;
        this.studentNamePort = studentNamePort;
        this.programNamePort = programNamePort;
    }

    @Override
    public List<DelegatedMembershipDto> execute(UUID tenantId, UUID programId) {
        // Query memberships in PENDING_MANAGER_ACTIVATION scoped to the program
        var page = membershipRepository.findAll(
                tenantId, null, programId,
                MembershipStatus.PENDING_MANAGER_ACTIVATION, 0, 100);

        List<DelegatedMembershipDto> result = new ArrayList<>();

        for (Membership m : page.getContent()) {
            String studentName = studentNamePort
                    .findFullName(m.getStudentId(), tenantId)
                    .orElse(m.getStudentId().toString());

            String programName = programNamePort
                    .findName(m.getProgramId(), tenantId)
                    .orElse(m.getProgramId().toString());

            Optional<DelegationReminder> reminder = delegationReminderAdapter
                    .findByMembershipId(m.getId().value());

            var delegatedAt = reminder.map(DelegationReminder::getDelegatedAt)
                    .orElse(m.getCreatedAt());

            // Resolve the proof ID for this membership
            List<PaymentProof> proofs = proofRepository
                    .findByMembershipId(tenantId, m.getId().value());

            UUID proofId = proofs.stream()
                    .filter(p -> "APPROVED".equals(p.getStatus().name()))
                    .findFirst()
                    .map(p -> p.getId().value())
                    .orElse(null);

            result.add(new DelegatedMembershipDto(
                    m.getId().value(), studentName, programName, delegatedAt, proofId));
        }

        return result;
    }
}
