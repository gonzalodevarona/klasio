package com.klasio.membership.domain.port;

import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository {

    void save(Membership membership);

    Optional<Membership> findById(UUID tenantId, UUID membershipId);

    Optional<Membership> findActiveByStudentIdAndProgramId(UUID tenantId, UUID studentId, UUID programId);

    Page<Membership> findAll(UUID tenantId, UUID studentId, UUID programId, MembershipStatus status, int page, int size);

    List<Membership> findAllByStudentIdAndProgramId(UUID tenantId, UUID studentId, UUID programId);

    /** Used by expiration job — returns ACTIVE and INACTIVE memberships past their expiration date */
    List<Membership> findExpirable(LocalDate today);

    /** Used by expiration job — returns ACTIVE memberships expiring within the given date range */
    List<Membership> findExpiringBetween(LocalDate from, LocalDate to);

    boolean existsActiveByStudentIdAndProgramId(UUID studentId, UUID programId);

    boolean existsActiveMembershipForStudent(UUID tenantId, UUID studentId);
}
