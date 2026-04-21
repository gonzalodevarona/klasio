package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.StudentProfilePort;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupInitiated;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterStudentServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentProfilePort studentProfilePort;
    @Mock private TenantResolverPort tenantResolverPort;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private AccountSetupTokenRepository accountSetupTokenRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RegisterStudentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RegisterStudentService(
                userRepository, studentProfilePort, tenantResolverPort,
                tokenGenerator, accountSetupTokenRepository, eventPublisher);
    }

    private RegisterStudentCommand buildCommand() {
        return new RegisterStudentCommand(
                "test-league",
                "Maria",
                "Lopez",
                LocalDate.of(1995, 6, 15),
                "CC",
                "9876543210",
                "Sanitas",
                "maria.lopez@example.com",
                null, null, null
        );
    }

    @Test
    @DisplayName("happy path: creates user via createPendingSetup, saves AccountSetupToken, publishes AccountSetupInitiated")
    void register_happyPath_savesTokenAndPublishesEvent() {
        String rawToken = "raw-token-abc";
        String hashedToken = "hashed-token-abc";

        when(tenantResolverPort.resolveTenantIdBySlug("test-league")).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId("maria.lopez@example.com", TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, "9876543210")).thenReturn(false);
        when(studentProfilePort.existsByIdentityNumberInTenant(TENANT_ID, "9876543210")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentProfilePort.createStudentProfile(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(STUDENT_ID);
        when(tokenGenerator.generateRawToken()).thenReturn(rawToken);
        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);

        service.register(buildCommand());

        // Verify user saved with null password (EMAIL_UNVERIFIED)
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isNull();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.EMAIL_UNVERIFIED);
        assertThat(savedUser.getFirstName()).isEqualTo("Maria");
        assertThat(savedUser.getLastName()).isEqualTo("Lopez");

        // Verify AccountSetupToken saved with ~15-min expiry
        ArgumentCaptor<AccountSetupToken> tokenCaptor = ArgumentCaptor.forClass(AccountSetupToken.class);
        verify(accountSetupTokenRepository).save(tokenCaptor.capture());
        AccountSetupToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getTokenHash()).isEqualTo(hashedToken);
        assertThat(savedToken.getUserId()).isEqualTo(savedUser.getId());
        Instant expectedExpiry = Instant.now().plus(15, ChronoUnit.MINUTES);
        assertThat(savedToken.getExpiresAt()).isBetween(
                expectedExpiry.minusSeconds(5), expectedExpiry.plusSeconds(5));

        // Verify AccountSetupInitiated published (not StudentRegisteredEvent)
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(AccountSetupInitiated.class);
        AccountSetupInitiated event = (AccountSetupInitiated) eventCaptor.getValue();
        assertThat(event.email()).isEqualTo("maria.lopez@example.com");
        assertThat(event.rawToken()).isEqualTo(rawToken);
        assertThat(event.role()).isEqualTo("student");
        assertThat(event.recipientName()).isEqualTo("Maria Lopez");
    }

    @Test
    @DisplayName("throws EmailAlreadyRegisteredException when email is already taken in the tenant")
    void register_duplicateEmail_throwsEmailAlreadyRegisteredException() {
        when(tenantResolverPort.resolveTenantIdBySlug("test-league")).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId("maria.lopez@example.com", TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.register(buildCommand()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
        verify(accountSetupTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("throws IdentityNumberAlreadyRegisteredException when identity number is already taken in the tenant")
    void register_duplicateIdentityNumber_throwsIdentityNumberAlreadyRegisteredException() {
        when(tenantResolverPort.resolveTenantIdBySlug("test-league")).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId("maria.lopez@example.com", TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, "9876543210")).thenReturn(true);

        assertThatThrownBy(() -> service.register(buildCommand()))
                .isInstanceOf(IdentityNumberAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
        verify(accountSetupTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("throws IdentityNumberAlreadyRegisteredException when identity number exists in student profiles")
    void register_identityNumberInStudentProfile_throwsIdentityNumberAlreadyRegisteredException() {
        when(tenantResolverPort.resolveTenantIdBySlug("test-league")).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId("maria.lopez@example.com", TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, "9876543210")).thenReturn(false);
        when(studentProfilePort.existsByIdentityNumberInTenant(TENANT_ID, "9876543210")).thenReturn(true);

        assertThatThrownBy(() -> service.register(buildCommand()))
                .isInstanceOf(IdentityNumberAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
        verify(accountSetupTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("throws IllegalArgumentException when tenant slug is not found")
    void register_unknownTenantSlug_throwsIllegalArgumentException() {
        when(tenantResolverPort.resolveTenantIdBySlug("test-league")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(buildCommand()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("test-league");

        verify(userRepository, never()).save(any());
        verify(accountSetupTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("throws IllegalArgumentException when identity document type is invalid")
    void register_invalidDocumentType_throwsIllegalArgumentException() {
        when(tenantResolverPort.resolveTenantIdBySlug("test-league")).thenReturn(Optional.of(TENANT_ID));
        when(userRepository.existsByEmailAndTenantId("maria.lopez@example.com", TENANT_ID)).thenReturn(false);

        RegisterStudentCommand command = new RegisterStudentCommand(
                "test-league", "Maria", "Lopez",
                LocalDate.of(1995, 6, 15),
                "INVALID_DOC_TYPE",
                "9876543210", "Sanitas", "maria.lopez@example.com",
                null, null, null);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }
}
