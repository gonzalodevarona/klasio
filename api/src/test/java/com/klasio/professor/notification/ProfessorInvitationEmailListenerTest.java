package com.klasio.professor.notification;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.professor.domain.event.ProfessorCreated;
import com.klasio.professor.infrastructure.notification.ProfessorInvitationEmailListener;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfessorInvitationEmailListenerTest {

    @Mock private EmailService emailService;
    @Mock private FrontendUrlBuilder urlBuilder;
    @Mock private TenantResolverPort tenantResolverPort;

    private ProfessorInvitationEmailListener listener;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROFESSOR_ID = UUID.randomUUID();
    private static final UUID INVITATION_TOKEN = UUID.randomUUID();
    private static final UUID CREATED_BY = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new ProfessorInvitationEmailListener(emailService, urlBuilder, tenantResolverPort);
    }

    @Test
    void onProfessorCreated_sendsInvitationEmail() {
        Instant expiresAt = Instant.now().plus(72, ChronoUnit.HOURS);

        when(tenantResolverPort.resolveSlugByTenantId(TENANT_ID)).thenReturn(Optional.of("liga-valle"));
        when(urlBuilder.build("liga-valle", "/activate-professor?token=" + INVITATION_TOKEN))
                .thenReturn("https://liga-valle.app.klasio.com/activate-professor?token=" + INVITATION_TOKEN);

        ProfessorCreated event = new ProfessorCreated(
                PROFESSOR_ID, TENANT_ID, "Carlos", "Reyes",
                "carlos@example.com", "3001234567",
                IdentityDocumentType.CC, "12345678",
                INVITATION_TOKEN, CREATED_BY,
                expiresAt, Instant.now());

        listener.onProfessorCreated(event);

        verify(emailService).send(
                eq(EmailType.PROFESSOR_INVITATION),
                eq(new EmailRecipient("carlos@example.com", "Carlos Reyes")),
                eq(TENANT_ID),
                argThat(params ->
                        "Carlos Reyes".equals(params.get("professorName")) &&
                        params.containsKey("activationUrl") &&
                        expiresAt.toString().equals(params.get("expiresAt"))));
    }

    @Test
    void onProfessorCreated_tenantSlugNotFound_usesAppFallback() {
        Instant expiresAt = Instant.now().plus(72, ChronoUnit.HOURS);

        when(tenantResolverPort.resolveSlugByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(urlBuilder.build("app", "/activate-professor?token=" + INVITATION_TOKEN))
                .thenReturn("http://localhost:3000/activate-professor?token=" + INVITATION_TOKEN);

        ProfessorCreated event = new ProfessorCreated(
                PROFESSOR_ID, TENANT_ID, "Carlos", "Reyes",
                "carlos@example.com", "3001234567",
                IdentityDocumentType.CC, "12345678",
                INVITATION_TOKEN, CREATED_BY,
                expiresAt, Instant.now());

        listener.onProfessorCreated(event);

        verify(emailService).send(
                eq(EmailType.PROFESSOR_INVITATION),
                any(EmailRecipient.class),
                eq(TENANT_ID),
                any());
    }
}
