package com.klasio.student.application.service;

import com.klasio.student.application.dto.StudentSummary;
import com.klasio.student.domain.model.BloodType;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.model.StudentId;
import com.klasio.student.domain.port.ActiveMembershipPort;
import com.klasio.student.domain.port.StudentRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListStudentsServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ActiveMembershipPort activeMembershipPort;

    private ListStudentsService service;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ListStudentsService(studentRepository, activeMembershipPort);
    }

    private Student reconstitute(String firstName, String lastName, String email) {
        return Student.reconstitute(
                StudentId.generate(), TENANT_ID, firstName, lastName, email,
                LocalDate.of(2000, 1, 15), "Sura", "1234567890", IdentityDocumentType.CC,
                BloodType.O_POSITIVE, "3001234567",
                null, null, null, null, null,
                "ACTIVE", Instant.now(), UUID.randomUUID(),
                null, null, null, null
        );
    }

    @Test
    @DisplayName("should return paginated student summaries with active membership flag")
    void shouldReturnPaginatedResults() {
        Student student1 = reconstitute("Carlos", "Garcia", "carlos@example.com");
        Student student2 = reconstitute("Maria", "Lopez", "maria@example.com");

        Page<Student> page = new PageImpl<>(List.of(student1, student2));
        when(studentRepository.findAll(TENANT_ID, 0, 10, null, null)).thenReturn(page);
        when(activeMembershipPort.hasActiveMembership(eq(TENANT_ID), eq(student1.getId().value()))).thenReturn(true);
        when(activeMembershipPort.hasActiveMembership(eq(TENANT_ID), eq(student2.getId().value()))).thenReturn(false);

        Page<StudentSummary> result = service.execute(TENANT_ID, 0, 10, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).firstName()).isEqualTo("Carlos");
        assertThat(result.getContent().get(0).hasActiveMembership()).isTrue();
        assertThat(result.getContent().get(1).firstName()).isEqualTo("Maria");
        assertThat(result.getContent().get(1).hasActiveMembership()).isFalse();

        verify(studentRepository).findAll(TENANT_ID, 0, 10, null, null);
        verify(activeMembershipPort).hasActiveMembership(TENANT_ID, student1.getId().value());
        verify(activeMembershipPort).hasActiveMembership(TENANT_ID, student2.getId().value());
    }

    @Test
    @DisplayName("should return empty page when no students match")
    void shouldReturnEmptyPage() {
        Page<Student> emptyPage = new PageImpl<>(List.of());
        when(studentRepository.findAll(TENANT_ID, 0, 10, "ACTIVE", "search"))
                .thenReturn(emptyPage);

        Page<StudentSummary> result = service.execute(TENANT_ID, 0, 10, "ACTIVE", "search");

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
