package com.klasio.programclass.infrastructure.web;

import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.application.port.input.ListClassesUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.student.application.dto.EnrollmentSummary;
import com.klasio.student.application.port.input.ListEnrollmentsUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests that MeClassesController includes OPEN-level classes for each enrolled program,
 * and deduplicates OPEN classes when a student has multiple enrollments in the same program.
 */
@ExtendWith(MockitoExtension.class)
class MeClassesControllerOpenLevelTest {

    @Mock private StudentIdPort studentIdPort;
    @Mock private ListEnrollmentsUseCase listEnrollmentsUseCase;
    @Mock private ListClassesUseCase listClassesUseCase;

    @InjectMocks
    private MeClassesController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();

    private static final UUID BEGINNER_CLASS_ID = UUID.randomUUID();
    private static final UUID ADVANCED_CLASS_ID = UUID.randomUUID();
    private static final UUID OPEN_CLASS_ID     = UUID.randomUUID();

    private void setupSecurityContext() {
        var auth = new UsernamePasswordAuthenticationToken("user", null, List.of());
        auth.setDetails(Map.of(
                "userId",   USER_ID.toString(),
                "tenantId", TENANT_ID.toString()
        ));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("returns enrollment-level class AND open-level class, excludes other levels")
    void mergesOpenClassesWithEnrollmentLevelClasses() {
        setupSecurityContext();

        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));

        EnrollmentSummary enrollment = new EnrollmentSummary(
                UUID.randomUUID(), STUDENT_ID, "Test Student",
                PROGRAM_ID, "Test Program", "BEGINNER",
                LocalDate.now(), "ACTIVE");

        when(listEnrollmentsUseCase.byStudent(TENANT_ID, STUDENT_ID, 0, 100, "ACTIVE"))
                .thenReturn(new PageImpl<>(List.of(enrollment)));

        ClassSummary beginnerClass = new ClassSummary(
                BEGINNER_CLASS_ID, PROGRAM_ID, "Test Program", "Beginner Class",
                "BEGINNER", "REGULAR", null, null, 15, "ACTIVE", Instant.now());

        ClassSummary openClass = new ClassSummary(
                OPEN_CLASS_ID, PROGRAM_ID, "Test Program", "Open Class",
                "OPEN", "REGULAR", null, null, 20, "ACTIVE", Instant.now());

        // BEGINNER fetch returns only the BEGINNER class
        when(listClassesUseCase.execute(eq(TENANT_ID), eq(PROGRAM_ID),
                eq(ClassLevel.BEGINNER), eq(ClassStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(beginnerClass)));

        // OPEN fetch returns the OPEN class (ADVANCED is never fetched, so never returned)
        when(listClassesUseCase.execute(eq(TENANT_ID), eq(PROGRAM_ID),
                eq(ClassLevel.OPEN), eq(ClassStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(openClass)));

        ResponseEntity<List<ClassResponseDto.ClassSummaryResponse>> response = controller.getMyClasses();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        List<ClassResponseDto.ClassSummaryResponse> body = response.getBody();
        assertThat(body).isNotNull();

        List<UUID> returnedIds = body.stream()
                .map(ClassResponseDto.ClassSummaryResponse::id)
                .toList();

        assertThat(returnedIds).contains(BEGINNER_CLASS_ID);
        assertThat(returnedIds).contains(OPEN_CLASS_ID);
        assertThat(returnedIds).doesNotContain(ADVANCED_CLASS_ID);
        assertThat(returnedIds).hasSize(2);
    }

    @Test
    @DisplayName("deduplicates OPEN class when student has two enrollments in the same program")
    void deduplicatesOpenClassWithMultipleEnrollmentsSameProgram() {
        setupSecurityContext();

        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));

        // Two enrollments in the same program at different levels
        EnrollmentSummary beginnerEnrollment = new EnrollmentSummary(
                UUID.randomUUID(), STUDENT_ID, "Test Student",
                PROGRAM_ID, "Test Program", "BEGINNER",
                LocalDate.now(), "ACTIVE");
        EnrollmentSummary intermediateEnrollment = new EnrollmentSummary(
                UUID.randomUUID(), STUDENT_ID, "Test Student",
                PROGRAM_ID, "Test Program", "INTERMEDIATE",
                LocalDate.now(), "ACTIVE");

        when(listEnrollmentsUseCase.byStudent(TENANT_ID, STUDENT_ID, 0, 100, "ACTIVE"))
                .thenReturn(new PageImpl<>(List.of(beginnerEnrollment, intermediateEnrollment)));

        ClassSummary beginnerClass = new ClassSummary(
                BEGINNER_CLASS_ID, PROGRAM_ID, "Test Program", "Beginner Class",
                "BEGINNER", "REGULAR", null, null, 15, "ACTIVE", Instant.now());

        ClassSummary intermediateClass = new ClassSummary(
                UUID.randomUUID(), PROGRAM_ID, "Test Program", "Intermediate Class",
                "INTERMEDIATE", "REGULAR", null, null, 15, "ACTIVE", Instant.now());

        ClassSummary openClass = new ClassSummary(
                OPEN_CLASS_ID, PROGRAM_ID, "Test Program", "Open Class",
                "OPEN", "REGULAR", null, null, 20, "ACTIVE", Instant.now());

        when(listClassesUseCase.execute(eq(TENANT_ID), eq(PROGRAM_ID),
                eq(ClassLevel.BEGINNER), eq(ClassStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(beginnerClass)));

        when(listClassesUseCase.execute(eq(TENANT_ID), eq(PROGRAM_ID),
                eq(ClassLevel.INTERMEDIATE), eq(ClassStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(intermediateClass)));

        // OPEN is returned for both enrollment iterations — dedup must prevent double entry
        when(listClassesUseCase.execute(eq(TENANT_ID), eq(PROGRAM_ID),
                eq(ClassLevel.OPEN), eq(ClassStatus.ACTIVE), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(openClass)));

        ResponseEntity<List<ClassResponseDto.ClassSummaryResponse>> response = controller.getMyClasses();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        List<ClassResponseDto.ClassSummaryResponse> body = response.getBody();
        assertThat(body).isNotNull();

        long openClassCount = body.stream()
                .filter(c -> c.id().equals(OPEN_CLASS_ID))
                .count();

        assertThat(openClassCount)
                .as("OPEN class must appear exactly once even with two enrollments in the same program")
                .isEqualTo(1);
    }
}
