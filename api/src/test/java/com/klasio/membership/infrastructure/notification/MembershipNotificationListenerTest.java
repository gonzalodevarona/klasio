package com.klasio.membership.infrastructure.notification;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.event.MembershipDepleted;
import com.klasio.membership.domain.event.MembershipExpiryWarning;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentEmailPort;
import com.klasio.membership.domain.port.StudentNamePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipNotificationListenerTest {

    @Mock private EmailService emailService;
    @Mock private StudentEmailPort studentEmailPort;
    @Mock private StudentNamePort studentNamePort;
    @Mock private ProgramNamePort programNamePort;

    private MembershipNotificationListener listener;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new MembershipNotificationListener(
                emailService, studentEmailPort, studentNamePort, programNamePort);
    }

    @Test
    void onMembershipActivated_sendsEmailToStudent() {
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("student@test.com"));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("John Doe"));
        when(programNamePort.findName(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of("Tennis Youth"));

        MembershipActivated event = new MembershipActivated(
                MEMBERSHIP_ID, TENANT_ID, STUDENT_ID, PROGRAM_ID, UUID.randomUUID(),
                "Monthly 10h", 10, LocalDate.now().plusDays(30), Instant.now());

        listener.onMembershipActivated(event);

        verify(emailService).send(
                eq(EmailType.MEMBERSHIP_ACTIVATED),
                eq(new EmailRecipient("student@test.com", "John Doe")),
                eq(TENANT_ID),
                argThat(params ->
                        "John Doe".equals(params.get("studentName")) &&
                        "Tennis Youth".equals(params.get("programName")) &&
                        "Monthly 10h".equals(params.get("planName")) &&
                        Integer.valueOf(10).equals(params.get("totalHours"))));
    }

    @Test
    void onMembershipActivated_noEmail_skips() {
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        MembershipActivated event = new MembershipActivated(
                MEMBERSHIP_ID, TENANT_ID, STUDENT_ID, PROGRAM_ID, UUID.randomUUID(),
                "Plan A", 8, LocalDate.now(), Instant.now());

        listener.onMembershipActivated(event);

        verifyNoInteractions(emailService);
    }

    @Test
    void onMembershipExpiryWarning_sendsEmailWithRemainingHours() {
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("student@test.com"));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("John Doe"));
        when(programNamePort.findName(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of("Tennis Youth"));

        MembershipExpiryWarning event = new MembershipExpiryWarning(
                MEMBERSHIP_ID, TENANT_ID, STUDENT_ID, PROGRAM_ID,
                LocalDate.now().plusDays(3), 4, Instant.now());

        listener.onMembershipExpiryWarning(event);

        verify(emailService).send(
                eq(EmailType.MEMBERSHIP_EXPIRY_WARNING),
                eq(new EmailRecipient("student@test.com", "John Doe")),
                eq(TENANT_ID),
                argThat(params ->
                        Integer.valueOf(4).equals(params.get("remainingHours"))));
    }

    @Test
    void onMembershipDepleted_sendsEmailToStudent() {
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("student@test.com"));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("John Doe"));
        when(programNamePort.findName(PROGRAM_ID, TENANT_ID)).thenReturn(Optional.of("Tennis Youth"));

        MembershipDepleted event = new MembershipDepleted(
                MEMBERSHIP_ID, TENANT_ID, STUDENT_ID, PROGRAM_ID, UUID.randomUUID(), Instant.now());

        listener.onMembershipDepleted(event);

        verify(emailService).send(
                eq(EmailType.MEMBERSHIP_DEPLETED),
                eq(new EmailRecipient("student@test.com", "John Doe")),
                eq(TENANT_ID),
                argThat(params ->
                        "John Doe".equals(params.get("studentName")) &&
                        "Tennis Youth".equals(params.get("programName"))));
    }
}
