package com.klasio.membership.infrastructure.notification;

import com.klasio.membership.domain.event.DelegationReminderDue;
import com.klasio.membership.domain.event.PaymentProofApproved;
import com.klasio.membership.domain.event.PaymentProofRejected;
import com.klasio.membership.domain.event.PaymentProofUploaded;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget listener for payment proof events that require external
 * notifications (email, in-app). Failures never block the triggering operation.
 *
 * Notifications are stubs pending RF-32 (Postmark integration).
 */
@Slf4j
@Component
public class PaymentProofNotificationListener {

    @Async
    @EventListener
    public void onPaymentProofUploaded(PaymentProofUploaded event) {
        log.info("[STUB] Notifying admin of new proof upload: proofId={} membershipId={} studentId={}",
                event.proofId(), event.membershipId(), event.studentId());
        // TODO: send in-app + email notification to admin (RF-32 / Postmark)
    }

    @Async
    @EventListener
    public void onPaymentProofApproved(PaymentProofApproved event) {
        if (event.activateDirectly()) {
            log.info("[STUB] Notifying student of membership activation: studentId={} membershipId={}",
                    event.studentId(), event.membershipId());
            // TODO: send activation confirmation email to student (RF-32 / Postmark)
        } else {
            log.info("[STUB] Notifying manager of delegation: membershipId={} tenantId={}",
                    event.membershipId(), event.tenantId());
            // TODO: send delegation notification to program manager (RF-32 / Postmark)
        }
    }

    @Async
    @EventListener
    public void onPaymentProofRejected(PaymentProofRejected event) {
        log.info("[STUB] Notifying student of proof rejection: studentId={} reason=\"{}\" membershipId={}",
                event.studentId(), event.rejectionReason(), event.membershipId());
        // TODO: send rejection reason via in-app + email to student (RF-32 / Postmark)
    }

    @Async
    @EventListener
    public void onDelegationReminderDue(DelegationReminderDue event) {
        log.info("[STUB] Notifying admin of overdue delegation: membershipId={} tenantId={}",
                event.membershipId(), event.tenantId());
        // TODO: send 48h reminder email to admin (RF-32 / Postmark)
    }
}
