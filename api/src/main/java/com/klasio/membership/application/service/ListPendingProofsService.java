package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.ListPendingProofsUseCase;
import com.klasio.membership.application.port.input.ProofQueueItemDto;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentNamePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListPendingProofsService implements ListPendingProofsUseCase {

    private final PaymentProofRepository proofRepository;
    private final StudentNamePort studentNamePort;
    private final ProgramNamePort programNamePort;

    public ListPendingProofsService(PaymentProofRepository proofRepository,
                                    StudentNamePort studentNamePort,
                                    ProgramNamePort programNamePort) {
        this.proofRepository = proofRepository;
        this.studentNamePort = studentNamePort;
        this.programNamePort = programNamePort;
    }

    @Override
    public List<ProofQueueItemDto> execute(UUID tenantId) {
        List<PaymentProof> pending = proofRepository.findPendingByTenantId(tenantId);

        return pending.stream()
                .map(p -> toDto(p, tenantId))
                .toList();
    }

    private ProofQueueItemDto toDto(PaymentProof p, UUID tenantId) {
        // Resolve membership's programId via a separate query would be ideal,
        // but we need it from the proof's membership. For simplicity, we use
        // the studentId as key for the student name port.
        String studentName = studentNamePort
                .findFullName(p.getStudentId(), tenantId)
                .orElse(p.getStudentId().toString());

        // ProgramName lookup requires programId; we store membershipId on proof.
        // The programNamePort lookup from membershipId is not directly available,
        // so we return a placeholder that the controller can enrich if needed.
        // This will be enriched in future if ProgramNamePort is extended.
        String programName = p.getMembershipId().toString();

        return new ProofQueueItemDto(
                p.getId().value(),
                p.getMembershipId(),
                studentName,
                programName,
                p.getUploadedAt(),
                p.getContentType()
        );
    }
}
