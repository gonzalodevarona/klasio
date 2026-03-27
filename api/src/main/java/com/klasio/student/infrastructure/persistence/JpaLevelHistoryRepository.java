package com.klasio.student.infrastructure.persistence;

import com.klasio.shared.infrastructure.persistence.TenantScopedRepository;
import com.klasio.student.domain.model.LevelHistoryEntry;
import com.klasio.student.domain.port.LevelHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JpaLevelHistoryRepository extends TenantScopedRepository implements LevelHistoryRepository {

    private final SpringDataLevelHistoryRepository springDataRepository;
    private final LevelHistoryMapper mapper;

    public JpaLevelHistoryRepository(SpringDataLevelHistoryRepository springDataRepository,
                                     LevelHistoryMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public void save(LevelHistoryEntry entry) {
        applyTenantContext();
        LevelHistoryJpaEntity entity = mapper.toEntity(entry);
        if (!springDataRepository.existsById(entity.getId())) {
            entity.markAsNew();
        }
        springDataRepository.save(entity);
    }

    @Override
    public Page<LevelHistoryEntry> findByEnrollmentId(UUID tenantId, UUID enrollmentId, int page, int size) {
        applyTenantContext();
        Pageable pageable = PageRequest.of(page, size);
        return springDataRepository.findByTenantIdAndEnrollmentIdOrderByChangedAtAsc(tenantId, enrollmentId, pageable)
                .map(mapper::toDomain);
    }
}
