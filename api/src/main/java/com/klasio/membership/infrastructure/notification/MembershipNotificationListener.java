package com.klasio.membership.infrastructure.notification;

import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.event.MembershipDepleted;
import com.klasio.membership.domain.event.MembershipExpired;
import com.klasio.membership.domain.event.MembershipExpiryWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget listener for membership lifecycle events that require
 * external notifications (email, in-app). Runs asynchronously to avoid
 * slowing down the transactional flow.
 *
 * In v1.0 these log stubs will be replaced with real Postmark email calls.
 */
@Component
public class MembershipNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(MembershipNotificationListener.class);

    @Async
    @EventListener
    public void onMembershipActivated(MembershipActivated event) {
        log.info("[NOTIFY] Membership activated — studentId={}, programId={}, membershipId={}",
                event.studentId(), event.programId(), event.membershipId());
        // TODO: send activation confirmation email to student (Postmark)
    }

    @Async
    @EventListener
    public void onMembershipDepleted(MembershipDepleted event) {
        log.info("[NOTIFY] Membership depleted (hours = 0) — studentId={}, programId={}, membershipId={}",
                event.studentId(), event.programId(), event.membershipId());
        // TODO: send depletion notification to student and manager (Postmark)
    }

    @Async
    @EventListener
    public void onMembershipExpired(MembershipExpired event) {
        log.info("[NOTIFY] Membership expired — studentId={}, programId={}, membershipId={}",
                event.studentId(), event.programId(), event.membershipId());
        // TODO: send expiration notice to student (Postmark)
    }

    @Async
    @EventListener
    public void onMembershipExpiryWarning(MembershipExpiryWarning event) {
        log.info("[NOTIFY] Membership expiry warning — studentId={}, programId={}, expirationDate={}, membershipId={}",
                event.studentId(), event.programId(), event.expirationDate(), event.membershipId());
        // TODO: send 3-day warning email to student (Postmark)
    }
}
