package com.klasio.student.application.dto;

import com.klasio.student.domain.model.LevelHistoryEntry;

import java.time.Instant;
import java.util.UUID;

public record LevelHistoryDetail(
        UUID id,
        String previousLevel,
        String newLevel,
        String action,
        UUID changedBy,
        String changedByRole,
        Instant changedAt,
        String justification
) {
    public static LevelHistoryDetail fromDomain(LevelHistoryEntry entry) {
        return new LevelHistoryDetail(
                entry.getId(),
                entry.getPreviousLevel() != null ? entry.getPreviousLevel().name() : null,
                entry.getNewLevel() != null ? entry.getNewLevel().name() : null,
                entry.getAction().name(),
                entry.getChangedBy(),
                entry.getChangedByRole(),
                entry.getChangedAt(),
                entry.getJustification()
        );
    }
}
