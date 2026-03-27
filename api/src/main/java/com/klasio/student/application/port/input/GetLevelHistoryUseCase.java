package com.klasio.student.application.port.input;

import com.klasio.student.application.dto.LevelHistoryDetail;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface GetLevelHistoryUseCase {
    Page<LevelHistoryDetail> execute(UUID tenantId, UUID enrollmentId, int page, int size);
}
