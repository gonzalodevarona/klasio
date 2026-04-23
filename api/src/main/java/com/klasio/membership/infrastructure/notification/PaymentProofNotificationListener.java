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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

/**
 * Transactional event listener for payment proof events that require email notifications.
 * Fires after the originating transaction commits so emails are never sent for rolled-back ops.
 * Runs on the dedicated emailListenerExecutor so failures never block the caller.
 *
 * Covered events:
 *  - PaymentProofUploaded  → notify all tenant admins to review the queue
 *  - PaymentProofRejected  → notify the student with rejection reason and retry link
 *
 * PaymentProofApproved and DelegationReminderDue are intentionally omitted:
 *  - Approval confirmation is covered by MembershipActivated (fired after actual activation)
 *  - Delegation reminders use in-app notifications only (per RF-32 scope)
 */
@Slf4j
@Component
public class PaymentProofNotificationListener {

    private final EmailService emailService;
    private final StudentEmailPort studentEmailPort;
    private final StudentNamePort studentNamePort;
    private final TenantAdminEmailPort tenantAdminEmailPort;
    private final MembershipPlanSnapshotPort planSnapshotPort;
    private final FrontendUrlBuilder urlBuilder;

    public PaymentProofNotificationListener(EmailService emailService,
                                            StudentEmailPort studentEmailPort,
                                            StudentNamePort studentNamePort,
                                            TenantAdminEmailPort tenantAdminEmailPort,
                                            MembershipPlanSnapshotPort planSnapshotPort,
                                            FrontendUrlBuilder urlBuilder) {
        this.emailService = emailService;
        this.studentEmailPort = studentEmailPort;
        this.studentNamePort = studentNamePort;
        this.tenantAdminEmailPort = tenantAdminEmailPort;
        this.planSnapshotPort = planSnapshotPort;
        this.urlBuilder = urlBuilder;
    }

    /**
     * Notifies all tenant admins when a student uploads a payment proof so they can review it promptly.
     */
    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentProofUploaded(PaymentProofUploaded event) {
        String studentName = studentNamePort.findFullName(event.studentId(), event.tenantId())
                .orElse("A student");
        String programName = planSnapshotPort.findSnapshot(event.membershipId(), event.tenantId())
                .map(MembershipPlanSnapshotPort.PlanSnapshot::programName).orElse("their program");
        String reviewUrl = urlBuilder.build("app", "/payment-proofs");

        for (String adminEmail : tenantAdminEmailPort.findAdminEmails(event.tenantId())) {
            emailService.send(
                    EmailType.PAYMENT_PROOF_UPLOADED,
                    new EmailRecipient(adminEmail, adminEmail),
                    event.tenantId(),
                    Map.of("studentName", studentName,
                           "programName", programName,
                           "reviewUrl", reviewUrl));
        }
    }

    /**
     * Notifies the student when their payment proof is rejected, including the reason and a retry link.
     */
    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentProofRejected(PaymentProofRejected event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) {
            log.warn("[payment-proof] skipping rejection email — no email on file for studentId={}", event.studentId());
            return;
        }
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String programName = planSnapshotPort.findSnapshot(event.membershipId(), event.tenantId())
                .map(MembershipPlanSnapshotPort.PlanSnapshot::programName).orElse("your program");
        String retryUrl = urlBuilder.build("app", "/memberships");

        emailService.send(
                EmailType.PAYMENT_REJECTED,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name,
                       "programName", programName,
                       "reason", event.rejectionReason(),
                       "retryUrl", retryUrl));
    }
}
