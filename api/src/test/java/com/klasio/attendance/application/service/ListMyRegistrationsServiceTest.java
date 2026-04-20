package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceRegistrationView;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMyRegistrationsServiceTest {

    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ClassDetailsPort classDetailsPort;
    @Mock ClassSessionRepository sessionRepository;

    @InjectMocks ListMyRegistrationsService service;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final Pageable PAGE = PageRequest.of(0, 20);

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private AttendanceRegistration buildRegistration() {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.generate(),
                TENANT_ID,
                UUID.randomUUID(),       // sessionId
                UUID.randomUUID(),       // classId
                STUDENT_ID,
                UUID.randomUUID(),       // enrollmentId
                UUID.randomUUID(),       // membershipId
                "BEGINNER",
                1,
                AttendanceRegistrationStatus.REGISTERED,
                LocalDate.now().plusDays(5),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null, null, null,   // cancelledAt, cancelledBy, cancellationReason
                null, null,         // markedAt, markedBy
                null, null, null,   // correctedAt, correctedBy, correctionReason
                Instant.now(), UUID.randomUUID(),
                null, null
        );
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    @Test
    @DisplayName("returns paginated page of views when student has registrations")
    void returnsPagedResults() {
        AttendanceRegistration reg = buildRegistration();
        Page<AttendanceRegistration> repoPage = new PageImpl<>(List.of(reg), PAGE, 1);

        when(registrationRepository.findByStudent(
                TENANT_ID, STUDENT_ID, null, null, null, null, PAGE))
                .thenReturn(repoPage);
        when(classDetailsPort.findClassName(any(), any())).thenReturn(Optional.of("Yoga Beginners"));
        when(sessionRepository.findByIds(any(), any())).thenReturn(List.of());

        Page<AttendanceRegistrationView> result =
                service.execute(TENANT_ID, STUDENT_ID, null, null, null, null, PAGE);

        assertThat(result.getTotalElements()).isEqualTo(1);
        AttendanceRegistrationView view = result.getContent().get(0);
        assertThat(view.studentId()).isEqualTo(STUDENT_ID);
        assertThat(view.intendedHours()).isEqualTo(1);
        assertThat(view.status()).isEqualTo("REGISTERED");
    }

    @Test
    @DisplayName("returns empty page when student has no registrations")
    void returnsEmptyPage() {
        Page<AttendanceRegistration> emptyPage = new PageImpl<>(List.of(), PAGE, 0);

        when(registrationRepository.findByStudent(
                TENANT_ID, STUDENT_ID, null, null, null, null, PAGE))
                .thenReturn(emptyPage);

        Page<AttendanceRegistrationView> result =
                service.execute(TENANT_ID, STUDENT_ID, null, null, null, null, PAGE);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("status filter is delegated to repository unchanged")
    void statusFilterDelegatedToRepository() {
        AttendanceRegistrationStatus status = AttendanceRegistrationStatus.REGISTERED;
        Page<AttendanceRegistration> page = new PageImpl<>(List.of(), PAGE, 0);

        when(registrationRepository.findByStudent(
                eq(TENANT_ID), eq(STUDENT_ID),
                isNull(), isNull(),
                eq(status), isNull(),
                eq(PAGE)))
                .thenReturn(page);

        service.execute(TENANT_ID, STUDENT_ID, null, null, status, null, PAGE);

        verify(registrationRepository).findByStudent(
                eq(TENANT_ID), eq(STUDENT_ID),
                isNull(), isNull(),
                eq(status), isNull(),
                eq(PAGE));
    }

    @Test
    @DisplayName("programId filter is delegated to repository unchanged")
    void programIdFilterDelegatedToRepository() {
        Page<AttendanceRegistration> page = new PageImpl<>(List.of(), PAGE, 0);

        when(registrationRepository.findByStudent(
                eq(TENANT_ID), eq(STUDENT_ID),
                isNull(), isNull(),
                isNull(), eq(PROGRAM_ID),
                eq(PAGE)))
                .thenReturn(page);

        service.execute(TENANT_ID, STUDENT_ID, null, null, null, PROGRAM_ID, PAGE);

        verify(registrationRepository).findByStudent(
                eq(TENANT_ID), eq(STUDENT_ID),
                isNull(), isNull(),
                isNull(), eq(PROGRAM_ID),
                eq(PAGE));
    }

    @Test
    @DisplayName("date range filters are delegated to repository unchanged")
    void dateRangeFilterDelegatedToRepository() {
        LocalDate from = LocalDate.now();
        LocalDate to   = from.plusDays(7);
        Page<AttendanceRegistration> page = new PageImpl<>(List.of(), PAGE, 0);

        when(registrationRepository.findByStudent(
                eq(TENANT_ID), eq(STUDENT_ID),
                eq(from), eq(to),
                isNull(), isNull(),
                eq(PAGE)))
                .thenReturn(page);

        service.execute(TENANT_ID, STUDENT_ID, from, to, null, null, PAGE);

        verify(registrationRepository).findByStudent(
                eq(TENANT_ID), eq(STUDENT_ID),
                eq(from), eq(to),
                isNull(), isNull(),
                eq(PAGE));
    }
}
