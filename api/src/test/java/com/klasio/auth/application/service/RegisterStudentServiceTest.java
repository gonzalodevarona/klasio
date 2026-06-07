package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.exception.SelfRegistrationConflictException;
import com.klasio.auth.domain.exception.SelfRegistrationDisabledException;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentIdentityNumberAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.application.port.input.CreateStudentUseCase;
import com.klasio.student.domain.model.BloodType;
import com.klasio.student.domain.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterStudentServiceTest {

    @Mock
    private TenantResolverPort tenantResolverPort;

    @Mock
    private CreateStudentUseCase createStudentUseCase;

    private RegisterStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        service = new RegisterStudentService(tenantResolverPort, createStudentUseCase);
    }

    private RegisterStudentCommand fullCommand() {
        return new RegisterStudentCommand(
                "liga-test",
                "Ana", "Martinez",
                LocalDate.of(1998, 3, 20),
                "CC", "12345678",
                "Sanitas",
                "ana@example.com",
                "O+", "3001234567",
                "Pedro", "Martinez",
                "Father", "3009999999", "pedro@example.com"
        );
    }

    @Test
    @DisplayName("delegates to CreateStudentUseCase with all fields mapped correctly")
    void delegatesToCreateStudentWithFullFields() {
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test")).thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(true);
        when(createStudentUseCase.execute(any())).thenReturn(mock(Student.class));

        service.register(fullCommand());

        ArgumentCaptor<CreateStudentCommand> captor = ArgumentCaptor.forClass(CreateStudentCommand.class);
        verify(createStudentUseCase).execute(captor.capture());

        CreateStudentCommand cmd = captor.getValue();
        assertThat(cmd.tenantId()).isEqualTo(TENANT_ID);
        assertThat(cmd.firstName()).isEqualTo("Ana");
        assertThat(cmd.lastName()).isEqualTo("Martinez");
        assertThat(cmd.email()).isEqualTo("ana@example.com");
        assertThat(cmd.phone()).isEqualTo("3001234567");
        assertThat(cmd.bloodType()).isEqualTo(BloodType.O_POSITIVE);
        assertThat(cmd.identityDocumentType()).isEqualTo(IdentityDocumentType.CC);
        assertThat(cmd.identityNumber()).isEqualTo("12345678");
        assertThat(cmd.createdBy()).isEqualTo(SYSTEM_ACTOR);
        assertThat(cmd.tutorFirstName()).isEqualTo("Pedro");
        assertThat(cmd.tutorLastName()).isEqualTo("Martinez");
        assertThat(cmd.tutorRelationship()).isEqualTo("Father");
        assertThat(cmd.tutorPhone()).isEqualTo("3009999999");
        assertThat(cmd.tutorEmail()).isEqualTo("pedro@example.com");
    }

    @Test
    @DisplayName("throws TenantNotFoundException when slug is unknown")
    void throwsTenantNotFound_whenSlugUnknown() {
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(fullCommand()))
                .isInstanceOf(TenantNotFoundException.class);

        verify(createStudentUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("throws SelfRegistrationDisabledException when flag is off")
    void throwsDisabled_whenFlagOff() {
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test")).thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.register(fullCommand()))
                .isInstanceOf(SelfRegistrationDisabledException.class);

        verify(createStudentUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("maps email conflict from CreateStudentUseCase to non-enumerating SelfRegistrationConflictException")
    void mapsEmailConflictToGenericNonEnumeratingError() {
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test")).thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(true);
        when(createStudentUseCase.execute(any()))
                .thenThrow(new StudentEmailAlreadyExistsException("already exists"));

        assertThatThrownBy(() -> service.register(fullCommand()))
                .isInstanceOf(SelfRegistrationConflictException.class);
    }

    @Test
    @DisplayName("maps identity number conflict from CreateStudentUseCase to non-enumerating SelfRegistrationConflictException")
    void mapsIdentityConflictToGenericNonEnumeratingError() {
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test")).thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(true);
        when(createStudentUseCase.execute(any()))
                .thenThrow(new StudentIdentityNumberAlreadyExistsException("already exists"));

        assertThatThrownBy(() -> service.register(fullCommand()))
                .isInstanceOf(SelfRegistrationConflictException.class);
    }
}
