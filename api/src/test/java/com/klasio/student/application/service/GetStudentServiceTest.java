package com.klasio.student.application.service;

import com.klasio.shared.infrastructure.exception.StudentNotFoundException;
import com.klasio.student.domain.model.BloodType;
import com.klasio.student.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.model.StudentId;
import com.klasio.student.domain.port.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    private GetStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetStudentService(studentRepository);
    }

    @Test
    @DisplayName("should return student when found")
    void shouldReturnStudentWhenFound() {
        Student student = Student.reconstitute(
                StudentId.of(STUDENT_ID), TENANT_ID, "Carlos", "Garcia",
                "carlos@example.com",
                LocalDate.of(2000, 1, 15), "Sura", "1234567890", IdentityDocumentType.CC,
                BloodType.O_POSITIVE, "3001234567",
                null, null, null, null, null,
                "ACTIVE", Instant.now(), UUID.randomUUID(),
                null, null, null, null
        );

        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.of(student));

        Student result = service.execute(TENANT_ID, STUDENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId().value()).isEqualTo(STUDENT_ID);
        assertThat(result.getFirstName()).isEqualTo("Carlos");
        assertThat(result.getLastName()).isEqualTo("Garcia");
    }

    @Test
    @DisplayName("should throw StudentNotFoundException when student not found")
    void shouldThrowWhenStudentNotFound() {
        when(studentRepository.findById(TENANT_ID, STUDENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, STUDENT_ID))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining(STUDENT_ID.toString());
    }
}
