package com.klasio.auth;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.application.service.RegisterStudentService;
import com.klasio.auth.domain.exception.SelfRegistrationConflictException;
import com.klasio.auth.domain.exception.SelfRegistrationDisabledException;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.student.application.service.CreateStudentService;
import com.klasio.student.domain.model.BloodType;
import com.klasio.student.domain.model.Student;
import com.klasio.student.domain.port.AccountSetupCreationPort;
import com.klasio.student.domain.port.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-layer integration tests for the student self-registration flow.
 *
 * <p>These tests wire the real {@link RegisterStudentService} against the real
 * {@link CreateStudentService}, with only the I/O boundaries (repositories, external
 * ports) mocked. This verifies the full field-mapping and delegation chain end-to-end
 * without requiring a database or Docker.
 *
 * <p>Covers:
 * <ul>
 *   <li>Happy path: all fields (phone, bloodType) flow through both services and arrive
 *       in the saved {@link Student} aggregate; the account-setup port is invoked.</li>
 *   <li>Flag-off: {@code self_registration_enabled = false} → {@link SelfRegistrationDisabledException},
 *       no student is saved.</li>
 *   <li>Duplicate email: {@code StudentRepository} signals a collision →
 *       {@link SelfRegistrationConflictException} (non-enumerating, does not reveal the
 *       field that conflicted).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class RegisterStudentIntegrationTest {

    // ─── I/O boundary mocks ───────────────────────────────────────────────────

    @Mock
    private TenantResolverPort tenantResolverPort;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AccountSetupCreationPort accountSetupCreationPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // ─── Real services under test ─────────────────────────────────────────────

    private RegisterStudentService registerStudentService;

    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @BeforeEach
    void setUp() {
        // Wire real CreateStudentService first, then RegisterStudentService on top of it.
        // This mirrors the production wiring and exercises the full delegation chain.
        CreateStudentService createStudentService =
                new CreateStudentService(studentRepository, eventPublisher, accountSetupCreationPort);

        registerStudentService = new RegisterStudentService(tenantResolverPort, createStudentService);
    }

    // ─── Test 1: Happy path – full field parity ───────────────────────────────

    @Test
    @DisplayName("register maps phone and bloodType through both services and triggers account-setup port")
    void register_allFields_savedStudentHasPhoneAndBloodType_andAccountSetupPortInvoked() {
        // Arrange
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test"))
                .thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(true);
        when(studentRepository.existsByEmailInTenant(any(), any())).thenReturn(false);
        when(studentRepository.existsByIdentityNumberInTenant(any(), any())).thenReturn(false);

        UUID mockUserId = UUID.randomUUID();
        when(accountSetupCreationPort.createAndDispatchSetup(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(mockUserId);

        RegisterStudentCommand command = fullCommand(
                "ana@example.com", "O+", "3001234567");

        // Act
        registerStudentService.register(command);

        // Assert – CreateStudentService saves the student twice: once immediately after creation
        // and once again after linking the userId. Both calls carry the same phone + bloodType.
        // We verify against the first save (index 0) which contains the initial aggregate state.
        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository, times(2)).save(captor.capture());

        Student saved = captor.getAllValues().get(0);
        assertThat(saved.getPhone()).isEqualTo("3001234567");
        assertThat(saved.getBloodType()).isEqualTo(BloodType.O_POSITIVE);

        // Assert – account-setup port was called (this triggers the email dispatch via the
        // AccountSetupCreationSupport adapter, which publishes AccountSetupInitiated)
        verify(accountSetupCreationPort).createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any());
    }

    // ─── Test 2: Self-registration disabled → 403 ────────────────────────────

    @Test
    @DisplayName("register with flag off throws SelfRegistrationDisabledException, no student saved")
    void register_flagOff_throwsSelfRegistrationDisabledException_andSavesNothing() {
        // Arrange
        when(tenantResolverPort.resolveTenantIdBySlug("liga-closed"))
                .thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(false);

        RegisterStudentCommand command = fullCommand(
                "bob@example.com", null, null,
                "liga-closed");

        // Act + Assert
        assertThatThrownBy(() -> registerStudentService.register(command))
                .isInstanceOf(SelfRegistrationDisabledException.class);

        // No student must have been saved
        verify(studentRepository, never()).save(any());

        // Account-setup port must not have been called
        verify(accountSetupCreationPort, never()).createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any());
    }

    // ─── Test 3: Duplicate email → 409 (non-enumerating) ─────────────────────

    @Test
    @DisplayName("register with duplicate email wraps collision in non-enumerating SelfRegistrationConflictException")
    void register_duplicateEmail_throwsNonEnumeratingConflict() {
        // Arrange: repository reports the email already exists
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test"))
                .thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(true);
        when(studentRepository.existsByEmailInTenant(TENANT_ID, "carlos@example.com"))
                .thenReturn(true); // simulates duplicate

        RegisterStudentCommand command = fullCommand("carlos@example.com", "A+", "3009876543");

        // Act + Assert – SelfRegistrationConflictException is the non-enumerating wrapper
        assertThatThrownBy(() -> registerStudentService.register(command))
                .isInstanceOf(SelfRegistrationConflictException.class);

        // No student saved, no account-setup initiated
        verify(studentRepository, never()).save(any());
        verify(accountSetupCreationPort, never()).createAndDispatchSetup(
                any(), any(), any(), any(), any(), any(), any());
    }

    // ─── Test 4: Duplicate identity number → same non-enumerating 409 ────────

    @Test
    @DisplayName("register with duplicate identity number also wraps as non-enumerating SelfRegistrationConflictException")
    void register_duplicateIdentityNumber_throwsNonEnumeratingConflict() {
        // Arrange: email is new but identity number already exists
        when(tenantResolverPort.resolveTenantIdBySlug("liga-test"))
                .thenReturn(Optional.of(TENANT_ID));
        when(tenantResolverPort.isSelfRegistrationEnabled(TENANT_ID)).thenReturn(true);
        when(studentRepository.existsByEmailInTenant(any(), any())).thenReturn(false);
        when(studentRepository.existsByIdentityNumberInTenant(TENANT_ID, "12345678"))
                .thenReturn(true); // simulates duplicate

        RegisterStudentCommand command = fullCommand("new@example.com", null, null);

        assertThatThrownBy(() -> registerStudentService.register(command))
                .isInstanceOf(SelfRegistrationConflictException.class);

        verify(studentRepository, never()).save(any());
    }

    // ─── Fixture helpers ──────────────────────────────────────────────────────

    /** Builds a full adult registration command targeting {@code liga-test}. */
    private RegisterStudentCommand fullCommand(String email, String bloodType, String phone) {
        return fullCommand(email, bloodType, phone, "liga-test");
    }

    /** Builds a full adult registration command targeting the given slug. */
    private RegisterStudentCommand fullCommand(String email, String bloodType, String phone,
                                               String tenantSlug) {
        return new RegisterStudentCommand(
                tenantSlug,
                "Ana", "Martinez",
                LocalDate.of(1998, 3, 20),
                IdentityDocumentType.CC.name(), "12345678",
                "Sanitas", email,
                bloodType, phone,
                null, null, null, null, null // no tutor (adult)
        );
    }
}
