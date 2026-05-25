package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListMyRegistrationsLocationTest {

    private final AttendanceRegistrationRepository registrationRepository = mock(AttendanceRegistrationRepository.class);
    private final ClassDetailsPort classDetailsPort = mock(ClassDetailsPort.class);
    private final ClassSessionRepository sessionRepository = mock(ClassSessionRepository.class);

    private final ListMyRegistrationsService service =
            new ListMyRegistrationsService(registrationRepository, classDetailsPort, sessionRepository);

    @Test
    void resolvesLocationFromMatchingScheduleEntry() {
        UUID tenantId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        LocalDate monday = LocalDate.of(2026, 6, 1); // Monday
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        AttendanceRegistration reg = AttendanceRegistration.register(
                UUID.randomUUID(), tenantId, classId, studentId,
                UUID.randomUUID(), UUID.randomUUID(), "BEGINNER", 1, 60,
                monday, start, end, UUID.randomUUID());

        Page<AttendanceRegistration> page = new PageImpl<>(List.of(reg));
        when(registrationRepository.findByStudent(eq(tenantId), eq(studentId), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(classDetailsPort.findClassName(tenantId, classId)).thenReturn(Optional.of("Yoga"));
        when(sessionRepository.findByIds(eq(tenantId), any())).thenReturn(List.of());
        when(classDetailsPort.findForRegistration(tenantId, classId)).thenReturn(Optional.of(
                new ClassRegistrationView(classId, UUID.randomUUID(), UUID.randomUUID(),
                        "BEGINNER", "ACTIVE", "RECURRING", 20, "Yoga",
                        List.of(new ScheduleEntryView(DayOfWeek.MONDAY, null, start, end, "Salon 1")))));

        Page<AttendanceRegistrationView> result = service.execute(
                tenantId, studentId, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).location()).isEqualTo("Salon 1");
    }
}
