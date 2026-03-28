package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.infrastructure.exception.MembershipAlreadyActiveException;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaMembershipRepository extends TenantScopedRepository implements MembershipRepository {

    private final SpringDataMembershipRepository springDataRepository;
    private final MembershipMapper mapper;

    public JpaMembershipRepository(SpringDataMembershipRepository springDataRepository,
                                    MembershipMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(Membership membership) {
        applyTenantContext();
        MembershipJpaEntity entity = mapper.toEntity(membership);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        try {
            springDataRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // Partial unique index violation: active or pending-manager membership already exists
            throw new MembershipAlreadyActiveException(
                    "Student already has an active or pending-manager-activation membership in this program");
        }
    }

    @Override
    public Optional<Membership> findById(UUID tenantId, UUID membershipId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, membershipId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Membership> findActiveByStudentIdAndProgramId(UUID tenantId, UUID studentId, UUID programId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndStudentIdAndProgramIdAndStatus(
                        tenantId, studentId, programId, MembershipStatus.ACTIVE.name())
                .map(mapper::toDomain);
    }

    @Override
    public Page<Membership> findAll(UUID tenantId, UUID studentId, UUID programId,
                                    MembershipStatus status, int page, int size) {
        applyTenantContext();
        String statusStr = status != null ? status.name() : null;
        return springDataRepository.findAllWithFilters(
                        tenantId, studentId, programId, statusStr, PageRequest.of(page, size))
                .map(mapper::toDomain);
    }

    @Override
    public List<Membership> findAllByStudentIdAndProgramId(UUID tenantId, UUID studentId, UUID programId) {
        applyTenantContext();
        return springDataRepository
                .findAllByTenantIdAndStudentIdAndProgramIdOrderByStartDateDesc(tenantId, studentId, programId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Membership> findExpirable(LocalDate today) {
        return springDataRepository.findExpirable(today)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Membership> findExpiringBetween(LocalDate from, LocalDate to) {
        return springDataRepository.findExpiringBetween(from, to)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsActiveByStudentIdAndProgramId(UUID studentId, UUID programId) {
        applyTenantContext();
        return springDataRepository.existsByStudentIdAndProgramIdAndStatus(
                studentId, programId, MembershipStatus.ACTIVE.name());
    }

    @Override
    public boolean existsActiveMembershipForStudent(UUID tenantId, UUID studentId) {
        applyTenantContext();
        return springDataRepository.existsByTenantIdAndStudentIdAndStatus(
                tenantId, studentId, MembershipStatus.ACTIVE.name());
    }
}
