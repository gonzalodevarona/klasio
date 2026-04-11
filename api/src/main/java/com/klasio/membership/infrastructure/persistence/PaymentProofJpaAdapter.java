package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.PaymentProofId;
import com.klasio.membership.domain.model.ProofStatus;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentProofJpaAdapter extends TenantScopedRepository implements PaymentProofRepository {

    private final SpringDataPaymentProofRepository springDataRepository;

    public PaymentProofJpaAdapter(SpringDataPaymentProofRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public PaymentProof save(PaymentProof proof) {
        applyTenantContext();
        PaymentProofJpaEntity entity = toEntity(proof);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        return toDomain(springDataRepository.save(entity));
    }

    @Override
    public Optional<PaymentProof> findById(UUID tenantId, PaymentProofId id) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, id.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<PaymentProof> findActiveByMembershipId(UUID tenantId, UUID membershipId) {
        applyTenantContext();
        return springDataRepository.findActiveByTenantIdAndMembershipId(tenantId, membershipId)
                .map(this::toDomain);
    }

    @Override
    public Optional<PaymentProof> findPendingByMembershipId(UUID tenantId, UUID membershipId) {
        applyTenantContext();
        return springDataRepository.findPendingByTenantIdAndMembershipId(tenantId, membershipId)
                .map(this::toDomain);
    }

    @Override
    public List<PaymentProof> findPendingByTenantId(UUID tenantId) {
        applyTenantContext();
        return springDataRepository.findPendingByTenantIdOrderByUploadedAtAsc(tenantId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<PaymentProof> findByMembershipId(UUID tenantId, UUID membershipId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndMembershipIdOrderByUploadedAtDesc(tenantId, membershipId)
                .stream().map(this::toDomain).toList();
    }

    private PaymentProof toDomain(PaymentProofJpaEntity e) {
        return PaymentProof.reconstitute(
                PaymentProofId.of(e.getId()),
                e.getTenantId(),
                e.getMembershipId(),
                e.getStudentId(),
                e.getFileKey(),
                e.getOriginalFileName(),
                e.getContentType(),
                e.getFileSizeBytes(),
                ProofStatus.valueOf(e.getStatus()),
                e.getUploadedAt(),
                e.getValidatedBy(),
                e.getValidatedAt(),
                e.getRejectionReason(),
                e.getCreatedBy(),
                e.getCreatedAt()
        );
    }

    private PaymentProofJpaEntity toEntity(PaymentProof proof) {
        PaymentProofJpaEntity e = new PaymentProofJpaEntity();
        e.setId(proof.getId().value());
        e.setTenantId(proof.getTenantId());
        e.setMembershipId(proof.getMembershipId());
        e.setStudentId(proof.getStudentId());
        e.setFileKey(proof.getFileKey());
        e.setOriginalFileName(proof.getOriginalFileName());
        e.setContentType(proof.getContentType());
        e.setFileSizeBytes(proof.getFileSizeBytes());
        e.setStatus(proof.getStatus().name());
        e.setRejectionReason(proof.getRejectionReason());
        e.setUploadedAt(proof.getUploadedAt());
        e.setValidatedBy(proof.getValidatedBy());
        e.setValidatedAt(proof.getValidatedAt());
        e.setCreatedBy(proof.getCreatedBy());
        e.setCreatedAt(proof.getCreatedAt());
        return e;
    }
}
