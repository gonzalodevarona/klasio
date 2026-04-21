package com.klasio.auth.application;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.*;
import com.klasio.auth.application.service.RegisterStudentService;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.exception.PasswordPolicyViolationException;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.auth.infrastructure.config.AuthProperties;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.klasio.auth.domain.event.StudentRegisteredEvent;

@ExtendWith(MockitoExtension.class)
class RegisterStudentServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentProfilePort studentProfilePort;
    @Mock private TenantResolverPort tenantResolverPort;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private EmailVerificationTokenRepository evtRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RegisterStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String TENANT_SLUG = "test-league";
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties(24, 30, 5, 15, "noreply@klasio.com");
        service = new RegisterStudentService(
                userRepository, studentProfilePort, tenantResolverPort,
                passwordEncoder, tokenGenerator, evtRepository,
                authProperties, eventPublisher);
    }

    @Test
    void happyPath_adultStudent_createsUserAndStudentProfile() {
        RegisterStudentCommand command = adultStudentCommand();

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId(command.email(), TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(studentProfilePort.existsByIdentityNumberInTenant(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentProfilePort.createStudentProfile(eq(TENANT_ID), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString(),
                isNull(), isNull(), isNull(), any(UUID.class))).thenReturn(STUDENT_ID);
        when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
        when(tokenGenerator.hashToken("raw-token")).thenReturn("hashed-token");

        service.register(command);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserStatus.EMAIL_UNVERIFIED, savedUser.getStatus());
        assertEquals(command.email(), savedUser.getEmail());
        assertEquals(TENANT_ID, savedUser.getTenantId());
        assertEquals(IdentityDocumentType.CC, savedUser.getIdentityDocumentType());
        assertEquals(command.identityNumber(), savedUser.getIdentityNumber());

        verify(studentProfilePort).createStudentProfile(eq(TENANT_ID), eq(command.firstName()),
                eq(command.lastName()), eq(command.email()), eq(command.dateOfBirth()),
                eq(command.identityDocumentType()), eq(command.identityNumber()), eq(command.eps()),
                isNull(), isNull(), isNull(), any(UUID.class));

        verify(evtRepository).save(any());

        ArgumentCaptor<StudentRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(StudentRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        StudentRegisteredEvent event = eventCaptor.getValue();
        assertEquals(command.email(), event.email());
        assertEquals("John Doe", event.displayName());
        assertEquals("raw-token", event.rawToken());
        assertNotNull(event.expiresAt());
    }

    @Test
    void happyPath_minorStudent_createsWithTutorFields() {
        RegisterStudentCommand command = minorStudentCommand();

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId(command.email(), TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(studentProfilePort.existsByIdentityNumberInTenant(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentProfilePort.createStudentProfile(eq(TENANT_ID), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString(),
                eq("Maria Garcia"), eq("Mother"), eq("3001234567"), any(UUID.class))).thenReturn(STUDENT_ID);
        when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
        when(tokenGenerator.hashToken("raw-token")).thenReturn("hashed-token");

        service.register(command);

        verify(studentProfilePort).createStudentProfile(eq(TENANT_ID), eq(command.firstName()),
                eq(command.lastName()), eq(command.email()), eq(command.dateOfBirth()),
                eq(command.identityDocumentType()), eq(command.identityNumber()), eq(command.eps()),
                eq("Maria Garcia"), eq("Mother"), eq("3001234567"), any(UUID.class));
    }

    @Test
    void duplicateEmail_throwsEmailAlreadyRegisteredException() {
        RegisterStudentCommand command = adultStudentCommand();

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId(command.email(), TENANT_ID)).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register(command));

        verify(userRepository, never()).save(any());
        verify(studentProfilePort, never()).createStudentProfile(any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateIdentityNumberInUsers_throwsIdentityNumberAlreadyRegisteredException() {
        RegisterStudentCommand command = adultStudentCommand();

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId(command.email(), TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, command.identityNumber())).thenReturn(true);

        assertThrows(IdentityNumberAlreadyRegisteredException.class, () -> service.register(command));

        verify(userRepository, never()).save(any());
    }

    @Test
    void duplicateIdentityNumberInStudents_throwsIdentityNumberAlreadyRegisteredException() {
        RegisterStudentCommand command = adultStudentCommand();

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId(command.email(), TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(studentProfilePort.existsByIdentityNumberInTenant(TENANT_ID, command.identityNumber())).thenReturn(true);

        assertThrows(IdentityNumberAlreadyRegisteredException.class, () -> service.register(command));

        verify(userRepository, never()).save(any());
    }

    @Test
    void invalidTenantSlug_throwsIllegalArgumentException() {
        RegisterStudentCommand command = adultStudentCommand();

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.register(command));

        verify(userRepository, never()).save(any());
    }

    @Test
    void weakPassword_throwsPasswordPolicyViolationException() {
        RegisterStudentCommand command = new RegisterStudentCommand(
                TENANT_SLUG, "John", "Doe", LocalDate.of(2000, 1, 1),
                "CC", "123456789", "Sura", "john@example.com",
                "weak", null, null, null);

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId(command.email(), TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(studentProfilePort.existsByIdentityNumberInTenant(TENANT_ID, command.identityNumber())).thenReturn(false);

        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class, () -> service.register(command));
        assertFalse(ex.getViolations().isEmpty());
    }

    @Test
    void adultWithOptionalTutorFields_succeeds() {
        RegisterStudentCommand command = new RegisterStudentCommand(
                TENANT_SLUG, "John", "Doe", LocalDate.of(2000, 1, 1),
                "CC", "123456789", "Sura", "john@example.com",
                "SecurePass1!", "Optional Tutor", "Uncle", "3009999999");

        when(tenantResolverPort.resolveTenantIdBySlug(TENANT_SLUG)).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId(command.email(), TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(studentProfilePort.existsByIdentityNumberInTenant(TENANT_ID, command.identityNumber())).thenReturn(false);
        when(passwordEncoder.encode(command.password())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentProfilePort.createStudentProfile(eq(TENANT_ID), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString(),
                eq("Optional Tutor"), eq("Uncle"), eq("3009999999"), any(UUID.class))).thenReturn(STUDENT_ID);
        when(tokenGenerator.generateRawToken()).thenReturn("raw-token");
        when(tokenGenerator.hashToken("raw-token")).thenReturn("hashed-token");

        assertDoesNotThrow(() -> service.register(command));
        verify(studentProfilePort).createStudentProfile(eq(TENANT_ID), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString(),
                eq("Optional Tutor"), eq("Uncle"), eq("3009999999"), any(UUID.class));
    }

    private RegisterStudentCommand adultStudentCommand() {
        return new RegisterStudentCommand(
                TENANT_SLUG, "John", "Doe", LocalDate.of(2000, 1, 1),
                "CC", "123456789", "Sura", "john@example.com",
                "SecurePass1!", null, null, null);
    }

    private RegisterStudentCommand minorStudentCommand() {
        return new RegisterStudentCommand(
                TENANT_SLUG, "Carlos", "Ramirez", LocalDate.now().minusYears(14),
                "TI", "987654321", "Sanitas", "carlos@example.com",
                "SecurePass1!", "Maria Garcia", "Mother", "3001234567");
    }
}
