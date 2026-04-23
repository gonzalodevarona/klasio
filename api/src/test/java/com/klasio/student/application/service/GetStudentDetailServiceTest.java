package com.klasio.student.application.service;

import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.application.dto.StudentDetail;
import com.klasio.student.domain.model.BloodType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentDetailServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserDisplayNamePort userDisplayNamePort;

    private GetStudentDetailService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetStudentDetailService(studentRepository, userDisplayNamePort);
    }

    @Test
    @DisplayName("should resolve createdBy UUID to display name")
    void execute_resolvesCreatedByToDisplayName() {
        Student student = Student.create(
                TENANT_ID, "Juan", "Perez", "juan@example.com",
                LocalDate.of(2000, 1, 15), "SaludTotal", "123456",
                IdentityDocumentType.CC, BloodType.O_POSITIVE,
                "3001234567", null, null, null, null, null, CREATED_BY);

        when(studentRepository.findById(TENANT_ID, student.getId().value())).thenReturn(Optional.of(student));
        when(userDisplayNamePort.findDisplayName(CREATED_BY)).thenReturn(Optional.of("Admin Torres"));

        StudentDetail result = service.execute(TENANT_ID, student.getId().value());

        assertThat(result.createdBy()).isEqualTo("Admin Torres");
    }

    @Test
    @DisplayName("should fall back to UUID string when user not found")
    void execute_fallsBackToUuidStringWhenUserNotFound() {
        Student student = Student.create(
                TENANT_ID, "Juan", "Perez", "juan@example.com",
                LocalDate.of(2000, 1, 15), "SaludTotal", "123456",
                IdentityDocumentType.CC, BloodType.O_POSITIVE,
                "3001234567", null, null, null, null, null, CREATED_BY);

        when(studentRepository.findById(TENANT_ID, student.getId().value())).thenReturn(Optional.of(student));
        when(userDisplayNamePort.findDisplayName(CREATED_BY)).thenReturn(Optional.empty());

        StudentDetail result = service.execute(TENANT_ID, student.getId().value());

        assertThat(result.createdBy()).isEqualTo(CREATED_BY.toString());
    }

    @Test
    @DisplayName("should throw StudentNotFoundException when student does not exist")
    void execute_studentNotFound_throwsException() {
        when(studentRepository.findById(TENANT_ID, STUDENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, STUDENT_ID))
                .isInstanceOf(StudentNotFoundException.class);
    }
}
