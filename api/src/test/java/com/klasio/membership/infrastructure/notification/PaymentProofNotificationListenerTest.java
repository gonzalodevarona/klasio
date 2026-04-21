package com.klasio.membership.infrastructure.notification;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.membership.domain.event.PaymentProofRejected;
import com.klasio.membership.domain.event.PaymentProofUploaded;
import com.klasio.membership.domain.port.MembershipPlanSnapshotPort;
import com.klasio.membership.domain.port.StudentEmailPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.membership.domain.port.TenantAdminEmailPort;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentProofNotificationListenerTest {

    @Mock private EmailService emailService;
    @Mock private StudentEmailPort studentEmailPort;
    @Mock private StudentNamePort studentNamePort;
    @Mock private TenantAdminEmailPort tenantAdminEmailPort;
    @Mock private MembershipPlanSnapshotPort planSnapshotPort;
    @Mock private FrontendUrlBuilder urlBuilder;

    private PaymentProofNotificationListener listener;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID PROOF_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        listener = new PaymentProofNotificationListener(
                emailService, studentEmailPort, studentNamePort,
                tenantAdminEmailPort, planSnapshotPort, urlBuilder);
    }

    @Test
    void onPaymentProofUploaded_sendsEmailToAllAdmins() {
        MembershipPlanSnapshotPort.PlanSnapshot snapshot = new MembershipPlanSnapshotPort.PlanSnapshot(
                "Monthly 10h", "Tennis Youth", 10, BigDecimal.TEN);
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("John Doe"));
        when(planSnapshotPort.findSnapshot(MEMBERSHIP_ID, TENANT_ID)).thenReturn(Optional.of(snapshot));
        when(tenantAdminEmailPort.findAdminEmails(TENANT_ID)).thenReturn(List.of("admin1@test.com", "admin2@test.com"));
        when(urlBuilder.build("app", "/payment-proofs")).thenReturn("http://localhost:3000/payment-proofs");

        PaymentProofUploaded event = new PaymentProofUploaded(PROOF_ID, TENANT_ID, MEMBERSHIP_ID, STUDENT_ID, Instant.now());

        listener.onPaymentProofUploaded(event);

        verify(emailService, times(2)).send(
                eq(EmailType.PAYMENT_PROOF_UPLOADED),
                any(EmailRecipient.class),
                eq(TENANT_ID),
                argThat(params ->
                        "John Doe".equals(params.get("studentName")) &&
                        "Tennis Youth".equals(params.get("programName"))));
    }

    @Test
    void onPaymentProofUploaded_noAdmins_sendsNothing() {
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("John Doe"));
        when(planSnapshotPort.findSnapshot(MEMBERSHIP_ID, TENANT_ID)).thenReturn(Optional.empty());
        when(tenantAdminEmailPort.findAdminEmails(TENANT_ID)).thenReturn(List.of());
        when(urlBuilder.build("app", "/payment-proofs")).thenReturn("http://localhost:3000/payment-proofs");

        PaymentProofUploaded event = new PaymentProofUploaded(PROOF_ID, TENANT_ID, MEMBERSHIP_ID, STUDENT_ID, Instant.now());

        listener.onPaymentProofUploaded(event);

        verifyNoInteractions(emailService);
    }

    @Test
    void onPaymentProofRejected_sendsEmailToStudent() {
        MembershipPlanSnapshotPort.PlanSnapshot snapshot = new MembershipPlanSnapshotPort.PlanSnapshot(
                "Monthly 10h", "Tennis Youth", 10, BigDecimal.TEN);
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("student@test.com"));
        when(studentNamePort.findFullName(STUDENT_ID, TENANT_ID)).thenReturn(Optional.of("John Doe"));
        when(planSnapshotPort.findSnapshot(MEMBERSHIP_ID, TENANT_ID)).thenReturn(Optional.of(snapshot));
        when(urlBuilder.build("app", "/memberships")).thenReturn("http://localhost:3000/memberships");

        PaymentProofRejected event = new PaymentProofRejected(
                PROOF_ID, TENANT_ID, MEMBERSHIP_ID, STUDENT_ID, "Blurry image", UUID.randomUUID(), Instant.now());

        listener.onPaymentProofRejected(event);

        verify(emailService).send(
                eq(EmailType.PAYMENT_REJECTED),
                eq(new EmailRecipient("student@test.com", "John Doe")),
                eq(TENANT_ID),
                argThat(params ->
                        "Blurry image".equals(params.get("reason")) &&
                        "John Doe".equals(params.get("studentName")) &&
                        "Tennis Youth".equals(params.get("programName"))));
    }

    @Test
    void onPaymentProofRejected_noEmail_skips() {
        when(studentEmailPort.findEmail(STUDENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        PaymentProofRejected event = new PaymentProofRejected(
                PROOF_ID, TENANT_ID, MEMBERSHIP_ID, STUDENT_ID, "Blurry image", UUID.randomUUID(), Instant.now());

        listener.onPaymentProofRejected(event);

        verifyNoInteractions(emailService);
    }
}
