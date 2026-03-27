package com.klasio.student.domain.port;

import com.klasio.student.domain.model.LevelHistoryEntry;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface LevelHistoryRepository {

    void save(LevelHistoryEntry entry);

    Page<LevelHistoryEntry> findByEnrollmentId(UUID tenantId, UUID enrollmentId, int page, int size);
}
