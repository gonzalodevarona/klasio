package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AvailableSessionView;
import com.klasio.attendance.application.port.input.GetAvailableSessionsUseCase;
import com.klasio.attendance.domain.port.EnrollmentLookupPort;
import com.klasio.attendance.domain.port.EnrollmentLookupPort.StudentEnrollmentView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMyAvailableSessionsServiceTest {

    @Mock EnrollmentLookupPort enrollmentLookupPort;
    @Mock GetAvailableSessionsUseCase getAvailableSessionsUseCase;

    @InjectMocks GetMyAvailableSessionsService service;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Test
    void noEnrollments_returnsEmptyList() {
        when(enrollmentLookupPort.findAllActiveEnrollmentsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(List.of());

        LocalDate from = LocalDate.now();
        LocalDate to   = from.plusDays(7);

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, from, to, false);

        assertThat(result).isEmpty();
        verifyNoInteractions(getAvailableSessionsUseCase);
    }

    @Test
    void multipleEnrollments_mergesAndSortsByDateThenStartTime() {
        UUID programA = UUID.randomUUID();
        UUID programB = UUID.randomUUID();

        when(enrollmentLookupPort.findAllActiveEnrollmentsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(List.of(
                        new StudentEnrollmentView(UUID.randomUUID(), programA, "BEGINNER"),
                        new StudentEnrollmentView(UUID.randomUUID(), programB, "INTERMEDIATE")
                ));

        LocalDate from = LocalDate.now();
        LocalDate to   = from.plusDays(7);

        AvailableSessionView a = sample(programA, from.plusDays(2), LocalTime.of(10, 0));
        AvailableSessionView b = sample(programB, from.plusDays(1), LocalTime.of(9, 0));
        AvailableSessionView c = sample(programA, from.plusDays(2), LocalTime.of(8, 0));

        when(getAvailableSessionsUseCase.execute(eq(TENANT_ID), eq(STUDENT_ID), eq(programA), any(), any(), anyBoolean()))
                .thenReturn(List.of(a, c));
        when(getAvailableSessionsUseCase.execute(eq(TENANT_ID), eq(STUDENT_ID), eq(programB), any(), any(), anyBoolean()))
                .thenReturn(List.of(b));

        List<AvailableSessionView> result = service.execute(TENANT_ID, STUDENT_ID, from, to, false);

        // Sorted: b (day+1, 09:00), c (day+2, 08:00), a (day+2, 10:00)
        assertThat(result).containsExactly(b, c, a);
        verify(getAvailableSessionsUseCase).execute(TENANT_ID, STUDENT_ID, programA, from, to, false);
        verify(getAvailableSessionsUseCase).execute(TENANT_ID, STUDENT_ID, programB, from, to, false);
    }

    private AvailableSessionView sample(UUID programId, LocalDate date, LocalTime startTime) {
        return new AvailableSessionView(
                UUID.randomUUID(), "Class", null, date, startTime, startTime.plusHours(1),
                "BEGINNER", programId, 0, 10, "SCHEDULED", true, null, null, null
        );
    }
}
