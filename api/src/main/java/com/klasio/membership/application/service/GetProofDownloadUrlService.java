package com.klasio.membership.application.service;

import com.klasio.membership.application.port.input.GetProofDownloadUrlUseCase;
import com.klasio.membership.domain.model.PaymentProofId;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.domain.port.PaymentProofStorage;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.shared.infrastructure.exception.PaymentProofNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProofDownloadUrlService implements GetProofDownloadUrlUseCase {

    private final PaymentProofRepository proofRepository;
    private final PaymentProofStorage proofStorage;
    private final StudentIdPort studentIdPort;

    public GetProofDownloadUrlService(PaymentProofRepository proofRepository,
                                      PaymentProofStorage proofStorage,
                                      StudentIdPort studentIdPort) {
        this.proofRepository = proofRepository;
        this.proofStorage = proofStorage;
        this.studentIdPort = studentIdPort;
    }

    @Override
    public String execute(UUID tenantId, UUID proofId, UUID actorId, String role) {
        var proof = proofRepository.findById(tenantId, PaymentProofId.of(proofId))
                .orElseThrow(() -> new PaymentProofNotFoundException(
                        "Payment proof not found: " + proofId));

        if ("STUDENT".equals(role)) {
            UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, actorId)
                    .orElseThrow(() -> new PaymentProofNotFoundException(
                            "Payment proof not found: " + proofId));
            if (!studentId.equals(proof.getStudentId())) {
                throw new AccessDeniedException("Students may only download their own payment proofs");
            }
        }

        return proofStorage.generateDownloadUrl(proof.getFileKey());
    }
}
