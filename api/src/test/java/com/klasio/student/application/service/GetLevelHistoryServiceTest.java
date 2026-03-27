package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.student.application.dto.LevelHistoryDetail;
import com.klasio.student.domain.model.Level;
import com.klasio.student.domain.model.LevelHistoryEntry;
import com.klasio.student.domain.model.StudentEnrollment;
import com.klasio.student.domain.model.StudentEnrollmentId;
import com.klasio.student.domain.port.LevelHistoryRepository;
import com.klasio.student.domain.port.StudentEnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class GetLevelHistoryServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private LevelHistoryRepository levelHistoryRepository;

    private GetLevelHistoryService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetLevelHistoryService(enrollmentRepository, levelHistoryRepository);
    }

    @Test
    @DisplayName("should return paginated history when enrollment exists")
    void shouldReturnPaginatedHistoryWhenEnrollmentExists() {
        StudentEnrollment enrollment = StudentEnrollment.reconstitute(
                StudentEnrollmentId.of(ENROLLMENT_ID), TENANT_ID, UUID.randomUUID(), UUID.randomUUID(),
                Level.BEGINNER, LocalDate.now(), "ACTIVE", Instant.now(), UUID.randomUUID(),
                null, null);

        UUID changedBy = UUID.randomUUID();
        LevelHistoryEntry entry1 = LevelHistoryEntry.reconstitute(
                UUID.randomUUID(), TENANT_ID, ENROLLMENT_ID, null, Level.BEGINNER,
                LevelHistoryEntry.Action.ENROLLED,
                changedBy, "ADMIN", Instant.now().minusSeconds(3600), null);
        LevelHistoryEntry entry2 = LevelHistoryEntry.reconstitute(
                UUID.randomUUID(), TENANT_ID, ENROLLMENT_ID, Level.BEGINNER, Level.INTERMEDIATE,
                LevelHistoryEntry.Action.PROMOTED,
                changedBy, "ADMIN", Instant.now(), "Student showed improvement");

        when(enrollmentRepository.findById(TENANT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.of(enrollment));
        when(levelHistoryRepository.findByEnrollmentId(TENANT_ID, ENROLLMENT_ID, 0, 10))
                .thenReturn(new PageImpl<>(List.of(entry1, entry2)));

        Page<LevelHistoryDetail> result = service.execute(TENANT_ID, ENROLLMENT_ID, 0, 10);

        assertThat(result.getContent()).hasSize(2);

        LevelHistoryDetail initialEntry = result.getContent().get(0);
        assertThat(initialEntry.previousLevel()).isNull();
        assertThat(initialEntry.newLevel()).isEqualTo("BEGINNER");

        LevelHistoryDetail promotionEntry = result.getContent().get(1);
        assertThat(promotionEntry.previousLevel()).isEqualTo("BEGINNER");
        assertThat(promotionEntry.newLevel()).isEqualTo("INTERMEDIATE");
        assertThat(promotionEntry.justification()).isEqualTo("Student showed improvement");

        verify(enrollmentRepository).findById(TENANT_ID, ENROLLMENT_ID);
        verify(levelHistoryRepository).findByEnrollmentId(TENANT_ID, ENROLLMENT_ID, 0, 10);
    }

    @Test
    @DisplayName("should throw EnrollmentNotFoundException when enrollment does not exist")
    void shouldThrowWhenEnrollmentNotFound() {
        when(enrollmentRepository.findById(TENANT_ID, ENROLLMENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, ENROLLMENT_ID, 0, 10))
                .isInstanceOf(EnrollmentNotFoundException.class)
                .hasMessageContaining(ENROLLMENT_ID.toString());

        verify(levelHistoryRepository, never()).findByEnrollmentId(any(), any(), anyInt(), anyInt());
    }
}
