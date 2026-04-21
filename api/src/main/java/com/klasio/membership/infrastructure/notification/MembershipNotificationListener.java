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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Slf4j
@Component
public class MembershipNotificationListener {

    private final EmailService emailService;
    private final StudentEmailPort studentEmailPort;
    private final StudentNamePort studentNamePort;
    private final ProgramNamePort programNamePort;

    public MembershipNotificationListener(EmailService emailService,
                                          StudentEmailPort studentEmailPort,
                                          StudentNamePort studentNamePort,
                                          ProgramNamePort programNamePort) {
        this.emailService = emailService;
        this.studentEmailPort = studentEmailPort;
        this.studentNamePort = studentNamePort;
        this.programNamePort = programNamePort;
    }

    // MembershipExpired is intentionally not handled here — expiration emails are not in scope for RF-32.
    // The 3-day expiry-warning email covers the pre-expiration student notification path.

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipActivated(MembershipActivated event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) {
            log.warn("[membership] skipping email — no email on file for studentId={}", event.studentId());
            return;
        }
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String program = programNamePort.findName(event.programId(), event.tenantId()).orElse("your program");

        emailService.send(
                EmailType.MEMBERSHIP_ACTIVATED,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name,
                       "programName", program,
                       "planName", event.planName(),
                       "totalHours", event.totalHours(),
                       "expiresAt", event.expirationDate().toString()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipExpiryWarning(MembershipExpiryWarning event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) {
            log.warn("[membership] skipping email — no email on file for studentId={}", event.studentId());
            return;
        }
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String program = programNamePort.findName(event.programId(), event.tenantId()).orElse("your program");

        emailService.send(
                EmailType.MEMBERSHIP_EXPIRY_WARNING,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name,
                       "programName", program,
                       "expiresAt", event.expirationDate().toString(),
                       "remainingHours", event.remainingHours()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipDepleted(MembershipDepleted event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) {
            log.warn("[membership] skipping email — no email on file for studentId={}", event.studentId());
            return;
        }
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String program = programNamePort.findName(event.programId(), event.tenantId()).orElse("your program");

        emailService.send(
                EmailType.MEMBERSHIP_DEPLETED,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name, "programName", program));
    }
}
