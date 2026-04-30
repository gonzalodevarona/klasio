package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.shared.infrastructure.exception.AlreadyRegisteredException;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JpaAttendanceRegistrationRepository extends TenantScopedRepository
        implements AttendanceRegistrationRepository {

    private final SpringDataAttendanceRegistrationRepository springDataRepository;
    private final AttendanceRegistrationMapper mapper;

    public JpaAttendanceRegistrationRepository(
            SpringDataAttendanceRegistrationRepository springDataRepository,
            AttendanceRegistrationMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(AttendanceRegistration registration) {
        applyTenantContext();
        AttendanceRegistrationJpaEntity entity = mapper.toEntity(registration);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        try {
            springDataRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // Partial unique index ux_registration_active_per_student_session violation
            throw new AlreadyRegisteredException(
                    "Student is already registered for this session");
        }
    }

    @Override
    public Optional<AttendanceRegistration> findById(UUID tenantId, UUID registrationId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, registrationId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<AttendanceRegistration> findByStudent(UUID tenantId, UUID studentId,
                                                       LocalDate from, LocalDate to,
                                                       AttendanceRegistrationStatus status,
                                                       UUID programId,
                                                       Pageable pageable) {
        applyTenantContext();
        String statusStr = status != null ? status.name() : null;
        return springDataRepository
                .findByStudentWithFilters(tenantId, studentId, from, to, statusStr, programId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Set<UUID> findRegisteredSessionIds(UUID tenantId, UUID studentId, List<UUID> sessionIds) {
        applyTenantContext();
        return Set.copyOf(springDataRepository.findRegisteredSessionIds(tenantId, studentId, sessionIds));
    }

    @Override
    public List<AttendanceRegistration> findByClassAndDateRange(UUID tenantId, UUID classId,
                                                                 LocalDate from, LocalDate to) {
        applyTenantContext();
        return springDataRepository.findByClassAndDateRange(tenantId, classId, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AttendanceRegistration> findFutureRegisteredByClass(UUID tenantId, UUID classId,
                                                                     LocalDate fromDate) {
        applyTenantContext();
        return springDataRepository.findFutureRegisteredByClass(tenantId, classId, fromDate)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AttendanceRegistration> findAllNonCancelledBySessionId(UUID tenantId, UUID sessionId) {
        applyTenantContext();
        List<String> excluded = List.of(
                AttendanceRegistrationStatus.CANCELLED_BY_STUDENT.name(),
                AttendanceRegistrationStatus.CANCELLED_BY_SYSTEM.name(),
                AttendanceRegistrationStatus.SESSION_CANCELLED.name()
        );
        return springDataRepository
                .findAllByTenantIdAndSessionIdAndStatusNotIn(tenantId, sessionId, excluded)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void saveAll(List<AttendanceRegistration> registrations) {
        applyTenantContext();
        List<AttendanceRegistrationJpaEntity> entities = registrations.stream()
                .map(mapper::toEntity)
                .toList();
        springDataRepository.saveAll(entities);
    }

    @Override
    public Map<UUID, RegistrationInfo> findActiveRegistrationsBySessionId(
            UUID tenantId, UUID studentId, LocalDate from, LocalDate to) {
        applyTenantContext();
        List<Object[]> rows = springDataRepository
                .findActiveRegistrationsInDateRange(tenantId, studentId, from, to);
        return rows.stream().collect(Collectors.toMap(
                row -> UUID.fromString(row[0].toString()),
                row -> new RegistrationInfo(
                        UUID.fromString(row[1].toString()),
                        row[2].toString()
                )
        ));
    }

    @Override
    public StatsProjection computeStatsForStudent(UUID tenantId, UUID studentId) {
        applyTenantContext();
        List<Object[]> rows = springDataRepository.computeStatsForStudent(tenantId, studentId);
        if (rows.isEmpty()) {
            return new StatsProjection(0L, 0L, 0L, 0L, 0L);
        }
        Object[] row = rows.get(0);
        return new StatsProjection(
                toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]), toLong(row[4])
        );
    }

    @Override
    public Optional<AttendanceRegistration> findActiveBySessionAndStudent(UUID tenantId,
                                                                           UUID sessionId,
                                                                           UUID studentId) {
        applyTenantContext();
        return springDataRepository.findActiveBySessionAndStudent(tenantId, sessionId, studentId)
                .map(mapper::toDomain);
    }

    @Override
    public Set<UUID> findActiveStudentIdsBySession(UUID tenantId, UUID sessionId) {
        applyTenantContext();
        return new HashSet<>(springDataRepository.findActiveStudentIdsBySession(tenantId, sessionId));
    }

    @Override
    public List<AttendanceRegistration> findFutureRegisteredForClass(UUID tenantId, UUID classId, Instant now) {
        applyTenantContext();
        return springDataRepository.findFutureRegisteredForClass(tenantId, classId, now)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }
}
