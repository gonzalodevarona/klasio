package com.klasio.membership.application.port.input;

import com.klasio.membership.domain.model.ProofStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentProofDto(
        UUID proofId,
        UUID membershipId,
        ProofStatus status,
        String originalFileName,
        Instant uploadedAt,
        String rejectionReason,  // nullable
        Instant validatedAt,     // nullable
        UUID validatedBy         // nullable
) {}
