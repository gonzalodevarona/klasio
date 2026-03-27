package com.klasio.student.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelHistoryEntryTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final Level NEW_LEVEL = Level.BEGINNER;
    private static final UUID CHANGED_BY = UUID.randomUUID();
    private static final String CHANGED_BY_ROLE = "ADMIN";

    @Nested
    @DisplayName("createInitial() factory")
    class CreateInitialFactory {

        @Test
        @DisplayName("should set previousLevel to null for initial entry")
        void shouldSetPreviousLevelToNull() {
            LevelHistoryEntry entry = LevelHistoryEntry.createInitial(
                    TENANT_ID, ENROLLMENT_ID, NEW_LEVEL, CHANGED_BY, CHANGED_BY_ROLE);

            assertThat(entry.getPreviousLevel()).isNull();
        }

        @Test
        @DisplayName("should set newLevel to provided level")
        void shouldSetNewLevel() {
            LevelHistoryEntry entry = LevelHistoryEntry.createInitial(
                    TENANT_ID, ENROLLMENT_ID, Level.INTERMEDIATE, CHANGED_BY, CHANGED_BY_ROLE);

            assertThat(entry.getNewLevel()).isEqualTo(Level.INTERMEDIATE);
        }

        @Test
        @DisplayName("should set changedAt to approximately now")
        void shouldSetChangedAtToNow() {
            Instant before = Instant.now();
            LevelHistoryEntry entry = LevelHistoryEntry.createInitial(
                    TENANT_ID, ENROLLMENT_ID, NEW_LEVEL, CHANGED_BY, CHANGED_BY_ROLE);
            Instant after = Instant.now();

            assertThat(entry.getChangedAt()).isNotNull();
            assertThat(entry.getChangedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("should generate a non-null id")
        void shouldGenerateId() {
            LevelHistoryEntry entry = LevelHistoryEntry.createInitial(
                    TENANT_ID, ENROLLMENT_ID, NEW_LEVEL, CHANGED_BY, CHANGED_BY_ROLE);

            assertThat(entry.getId()).isNotNull();
        }

        @Test
        @DisplayName("should store all provided fields")
        void shouldStoreAllFields() {
            LevelHistoryEntry entry = LevelHistoryEntry.createInitial(
                    TENANT_ID, ENROLLMENT_ID, NEW_LEVEL, CHANGED_BY, CHANGED_BY_ROLE);

            assertThat(entry.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(entry.getEnrollmentId()).isEqualTo(ENROLLMENT_ID);
            assertThat(entry.getChangedBy()).isEqualTo(CHANGED_BY);
            assertThat(entry.getChangedByRole()).isEqualTo(CHANGED_BY_ROLE);
            assertThat(entry.getJustification()).isNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should reject null newLevel")
        void shouldRejectNullNewLevel() {
            assertThatThrownBy(() -> LevelHistoryEntry.createInitial(
                    TENANT_ID, ENROLLMENT_ID, null, CHANGED_BY, CHANGED_BY_ROLE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("New level must not be null");
        }

        @Test
        @DisplayName("should reject null changedBy")
        void shouldRejectNullChangedBy() {
            assertThatThrownBy(() -> LevelHistoryEntry.createInitial(
                    TENANT_ID, ENROLLMENT_ID, NEW_LEVEL, null, CHANGED_BY_ROLE))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Changed by must not be null");
        }
    }
}
