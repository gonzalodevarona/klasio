package com.klasio.membership.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentProofRepository extends JpaRepository<PaymentProofJpaEntity, UUID> {

    Optional<PaymentProofJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("SELECT p FROM PaymentProofJpaEntity p WHERE p.tenantId = :tenantId AND p.membershipId = :membershipId AND p.status IN ('PENDING', 'APPROVED', 'REJECTED') ORDER BY p.uploadedAt DESC")
    Optional<PaymentProofJpaEntity> findActiveByTenantIdAndMembershipId(
            @Param("tenantId") UUID tenantId,
            @Param("membershipId") UUID membershipId);

    @Query("SELECT p FROM PaymentProofJpaEntity p WHERE p.tenantId = :tenantId AND p.membershipId = :membershipId AND p.status = 'PENDING'")
    Optional<PaymentProofJpaEntity> findPendingByTenantIdAndMembershipId(
            @Param("tenantId") UUID tenantId,
            @Param("membershipId") UUID membershipId);

    /**
     * Admin queue query. Returns only proofs that are still PENDING AND whose
     * associated membership is still in PENDING_PAYMENT_VALIDATION. This guards
     * the queue from stale rows — for example when the membership was validated
     * via the legacy ValidatePaymentService endpoint — so an approved/activated
     * membership never keeps a phantom entry in the admin queue.
     */
    @Query("""
            SELECT p FROM PaymentProofJpaEntity p, MembershipJpaEntity m
             WHERE p.tenantId = :tenantId
               AND p.status = 'PENDING'
               AND m.id = p.membershipId
               AND m.tenantId = p.tenantId
               AND m.status = 'PENDING_PAYMENT_VALIDATION'
             ORDER BY p.uploadedAt ASC
            """)
    List<PaymentProofJpaEntity> findPendingByTenantIdOrderByUploadedAtAsc(@Param("tenantId") UUID tenantId);

    List<PaymentProofJpaEntity> findByTenantIdAndMembershipIdOrderByUploadedAtDesc(UUID tenantId, UUID membershipId);
}
