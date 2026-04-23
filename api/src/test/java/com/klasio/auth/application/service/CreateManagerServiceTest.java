package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.CreateAdminCommand;
import com.klasio.auth.application.port.AccountSetupTokenRepository;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.port.TokenGenerator;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.event.AccountSetupInitiated;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.model.AccountSetupToken;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.domain.model.UserStatus;
import com.klasio.professor.domain.port.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateManagerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantNamePort tenantNamePort;
    @Mock private ProfessorRepository professorRepository;
    @Mock private AccountSetupTokenRepository accountSetupTokenRepository;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CreateManagerService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CreateManagerService(
                userRepository, tenantNamePort, professorRepository,
                accountSetupTokenRepository, tokenGenerator, eventPublisher);
    }

    private CreateAdminCommand buildCommand() {
        return new CreateAdminCommand(
                TENANT_ID, "ana.gomez@example.com",
                "CC", "55551234", "Ana", "Gomez", "+573109876543", CREATED_BY);
    }

    @Test
    @DisplayName("happy path: creates manager with no password, saves AccountSetupToken, publishes AccountSetupInitiated")
    void execute_happyPath_savesTokenAndPublishesEvent() {
        String rawToken = "raw-mgr-token";
        String hashedToken = "hashed-mgr-token";

        when(userRepository.existsByEmailAndTenantId("ana.gomez@example.com", TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, "55551234")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenGenerator.generateRawToken()).thenReturn(rawToken);
        when(tokenGenerator.hashToken(rawToken)).thenReturn(hashedToken);
        when(tenantNamePort.findNamesByIds(Set.of(TENANT_ID))).thenReturn(Map.of(TENANT_ID, "Test League"));

        AdminSummary result = service.execute(buildCommand());

        assertThat(result).isNotNull();
        assertThat(result.email()).isEqualTo("ana.gomez@example.com");

        // User must have null password and INVITED status
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isNull();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.INVITED);

        // AccountSetupToken saved with ~15-min expiry
        ArgumentCaptor<AccountSetupToken> tokenCaptor = ArgumentCaptor.forClass(AccountSetupToken.class);
        verify(accountSetupTokenRepository).save(tokenCaptor.capture());
        AccountSetupToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getTokenHash()).isEqualTo(hashedToken);
        Instant expectedExpiry = Instant.now().plus(15, ChronoUnit.MINUTES);
        assertThat(savedToken.getExpiresAt()).isBetween(
                expectedExpiry.minusSeconds(5), expectedExpiry.plusSeconds(5));

        // AccountSetupInitiated published
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(AccountSetupInitiated.class);
        AccountSetupInitiated event = (AccountSetupInitiated) eventCaptor.getValue();
        assertThat(event.email()).isEqualTo("ana.gomez@example.com");
        assertThat(event.rawToken()).isEqualTo(rawToken);
        assertThat(event.role()).isEqualTo("manager");
    }

    @Test
    @DisplayName("throws EmailAlreadyRegisteredException when email is already taken in tenant")
    void execute_duplicateEmail_throwsEmailAlreadyRegisteredException() {
        when(userRepository.existsByEmailAndTenantId("ana.gomez@example.com", TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(buildCommand()))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
        verify(accountSetupTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("throws IdentityNumberAlreadyRegisteredException when identity number is taken in tenant")
    void execute_duplicateIdentityNumber_throwsIdentityNumberAlreadyRegisteredException() {
        when(userRepository.existsByEmailAndTenantId("ana.gomez@example.com", TENANT_ID)).thenReturn(false);
        when(userRepository.existsByIdentityNumberAndTenantId(TENANT_ID, "55551234")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(buildCommand()))
                .isInstanceOf(IdentityNumberAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
        verify(accountSetupTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
