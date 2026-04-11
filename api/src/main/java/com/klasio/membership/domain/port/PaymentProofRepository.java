package com.klasio.membership.domain.port;

import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.PaymentProofId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentProofRepository {
    PaymentProof save(PaymentProof proof);
    Optional<PaymentProof> findById(UUID tenantId, PaymentProofId id);
    /**
     * Returns the active proof for a membership: status IN (PENDING, APPROVED, REJECTED).
     * After V040, multiple APPROVED/REJECTED proofs may exist (one per renewal cycle),
     * so this returns the most recent one.
     */
    Optional<PaymentProof> findActiveByMembershipId(UUID tenantId, UUID membershipId);
    /** Returns the current PENDING proof for a membership (at most one, enforced by partial unique index). */
    Optional<PaymentProof> findPendingByMembershipId(UUID tenantId, UUID membershipId);
    /** Returns all PENDING proofs for the tenant, ordered by uploadedAt ASC. */
    List<PaymentProof> findPendingByTenantId(UUID tenantId);
    /** Full proof history for a given membership (all statuses). */
    List<PaymentProof> findByMembershipId(UUID tenantId, UUID membershipId);
}
