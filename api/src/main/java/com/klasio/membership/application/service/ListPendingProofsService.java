package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.ListPendingProofsUseCase;
import com.klasio.membership.application.port.input.ProofQueueItemDto;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.MembershipPlanNamePort;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.domain.port.StudentProfilePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListPendingProofsService implements ListPendingProofsUseCase {

    private final PaymentProofRepository proofRepository;
    private final StudentProfilePort studentProfilePort;
    private final MembershipPlanNamePort membershipPlanNamePort;

    public ListPendingProofsService(PaymentProofRepository proofRepository,
                                    StudentProfilePort studentProfilePort,
                                    MembershipPlanNamePort membershipPlanNamePort) {
        this.proofRepository = proofRepository;
        this.studentProfilePort = studentProfilePort;
        this.membershipPlanNamePort = membershipPlanNamePort;
    }

    @Override
    public List<ProofQueueItemDto> execute(UUID tenantId) {
        List<PaymentProof> pending = proofRepository.findPendingByTenantId(tenantId);
        return pending.stream()
                .map(p -> toDto(p, tenantId))
                .toList();
    }

    private ProofQueueItemDto toDto(PaymentProof p, UUID tenantId) {
        StudentProfilePort.StudentProfile profile = studentProfilePort
                .findProfile(p.getStudentId(), tenantId)
                .orElse(new StudentProfilePort.StudentProfile(
                        p.getStudentId().toString(), "—", "—"));

        String planName = membershipPlanNamePort
                .findPlanName(p.getMembershipId(), tenantId)
                .orElse("—");

        return new ProofQueueItemDto(
                p.getId().value(),
                p.getMembershipId(),
                profile.fullName(),
                profile.identityDocumentType(),
                profile.identityNumber(),
                planName,
                p.getUploadedAt(),
                p.getContentType()
        );
    }
}
