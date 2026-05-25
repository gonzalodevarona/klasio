package com.klasio.membership.infrastructure.notification;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.membership.domain.event.MembershipLowHours;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentEmailPort;
import com.klasio.membership.domain.port.StudentNamePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipNotificationListenerLowHoursTest {

    @Mock private EmailService emailService;
    @Mock private StudentEmailPort studentEmailPort;
    @Mock private StudentNamePort studentNamePort;
    @Mock private ProgramNamePort programNamePort;

    private MembershipNotificationListener listener;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new MembershipNotificationListener(
                emailService, studentEmailPort, studentNamePort, programNamePort);
    }

    @Test
    void onMembershipLowHours_sendsEmailWithStudentNameAndRemainingHours() {
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("student@test.com"));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("John Doe"));

        MembershipLowHours event = new MembershipLowHours(
                MEMBERSHIP_ID, TENANT_ID, STUDENT_ID, 2, Instant.now());

        listener.onMembershipLowHours(event);

        verify(emailService).send(
                eq(EmailType.MEMBERSHIP_LOW_HOURS),
                eq(new EmailRecipient("student@test.com", "John Doe")),
                eq(TENANT_ID),
                argThat(params ->
                        "John Doe".equals(params.get("studentName")) &&
                        Integer.valueOf(2).equals(params.get("remainingHours"))));
    }

    @Test
    void onMembershipLowHours_noEmailOnFile_skips() {
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        MembershipLowHours event = new MembershipLowHours(
                MEMBERSHIP_ID, TENANT_ID, STUDENT_ID, 1, Instant.now());

        listener.onMembershipLowHours(event);

        verifyNoInteractions(emailService);
    }
}
