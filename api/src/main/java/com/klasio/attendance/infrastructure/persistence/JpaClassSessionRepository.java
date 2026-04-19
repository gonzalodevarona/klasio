package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaClassSessionRepository extends TenantScopedRepository implements ClassSessionRepository {

    private final SpringDataClassSessionRepository springDataRepository;
    private final ClassSessionMapper mapper;

    public JpaClassSessionRepository(SpringDataClassSessionRepository springDataRepository,
                                      ClassSessionMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(ClassSession session) {
        applyTenantContext();
        ClassSessionJpaEntity entity = mapper.toEntity(session);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Optional<ClassSession> findById(UUID tenantId, UUID sessionId) {
        applyTenantContext();
        return springDataRepository.findByTenantIdAndId(tenantId, sessionId)
                .map(mapper::toDomain);
    }

    @Override
    public ClassSession findOrCreate(UUID tenantId, UUID classId, LocalDate sessionDate,
                                     LocalTime startTime, LocalTime endTime, UUID actorId) {
        applyTenantContext();
        UUID newId = UUID.randomUUID();
        // Race-safe upsert: ON CONFLICT (class_id, session_date, start_time) DO NOTHING
        springDataRepository.tryInsertSession(newId, tenantId, classId, sessionDate, startTime, endTime, actorId);
        // Always SELECT by natural key — works whether we just inserted or it already existed
        return springDataRepository.findByClassIdAndSessionDateAndStartTime(classId, sessionDate, startTime)
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException(
                        "Session not found after upsert for classId=" + classId
                                + ", date=" + sessionDate + ", time=" + startTime));
    }

    @Override
    public boolean incrementCapacityIfSpace(UUID sessionId, int maxCapacity) {
        applyTenantContext();
        int rows = springDataRepository.incrementCapacityIfSpace(sessionId, maxCapacity);
        return rows > 0;
    }

    @Override
    public void decrementCapacity(UUID sessionId) {
        applyTenantContext();
        springDataRepository.decrementCapacity(sessionId);
    }

    @Override
    public List<ClassSession> findByClassIdsAndDateRange(UUID tenantId, List<UUID> classIds,
                                                          LocalDate from, LocalDate to) {
        if (classIds.isEmpty()) {
            return List.of();
        }
        applyTenantContext();
        return springDataRepository
                .findByTenantIdAndClassIdInAndSessionDateBetween(tenantId, classIds, from, to)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void cancelFutureSessionsByClass(UUID tenantId, UUID classId, LocalDate fromDate) {
        applyTenantContext();
        springDataRepository.cancelFutureSessionsByClass(tenantId, classId, fromDate);
    }

    @Override
    public void resetCurrentCapacity(UUID sessionId) {
        applyTenantContext();
        springDataRepository.resetCurrentCapacity(sessionId);
    }
}
