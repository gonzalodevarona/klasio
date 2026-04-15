package com.klasio.membership.infrastructure.web;

import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.ProofStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentProofResponseDto(
        UUID proofId,
        UUID membershipId,
        UUID studentId,
        ProofStatus status,
        String originalFileName,
        String contentType,
        long fileSizeBytes,
        Instant uploadedAt,
        String rejectionReason,
        Instant validatedAt,
        UUID validatedBy
) {
    public static PaymentProofResponseDto from(PaymentProof proof) {
        return new PaymentProofResponseDto(
                proof.getId().value(),
                proof.getMembershipId(),
                proof.getStudentId(),
                proof.getStatus(),
                proof.getOriginalFileName(),
                proof.getContentType(),
                proof.getFileSizeBytes(),
                proof.getUploadedAt(),
                proof.getRejectionReason(),
                proof.getValidatedAt(),
                proof.getValidatedBy()
        );
    }
}
