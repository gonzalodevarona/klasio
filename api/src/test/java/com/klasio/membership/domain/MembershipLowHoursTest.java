package com.klasio.membership.domain;

import com.klasio.membership.domain.event.MembershipDepleted;
import com.klasio.membership.domain.event.MembershipLowHours;
import com.klasio.membership.domain.model.Membership;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Low-hours warning behavior on the Membership aggregate (feature 016).
 * The warning fires once per lifecycle when the balance drops into the
 * (0, LOW_HOURS_THRESHOLD] window, and re-arms when the membership is
 * re-activated.
 */
class MembershipLowHoursTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.of(2026, 4, 15);

    /** Builds an ACTIVE, hours-based membership with the given balance. */
    private Membership activeWithHours(int hours) {
        Membership m = Membership.create(TENANT_ID, STUDENT_ID, ENROLLMENT_ID, PROGRAM_ID,
                PLAN_ID, "Test Plan", hours, ProgramModality.HOURS_BASED, START_DATE, ACTOR_ID);
        m.markProofUploaded();
        m.validatePayment(ACTOR_ID, true); // → ACTIVE
        m.clearDomainEvents();
        return m;
    }

    private long lowHoursCount(Membership m) {
        return m.getDomainEvents().stream().filter(e -> e instanceof MembershipLowHours).count();
    }

    @Test
    @DisplayName("crossing the threshold from above emits MembershipLowHours with the new balance")
    void deduct_crossesThreshold_emitsLowHours() {
        Membership m = activeWithHours(Membership.LOW_HOURS_THRESHOLD + 1); // 3 hours

        m.deductHours(1, ACTOR_ID, "PROFESSOR"); // → 2 (== threshold)

        List<DomainEvent> events = m.getDomainEvents();
        MembershipLowHours low = events.stream()
                .filter(e -> e instanceof MembershipLowHours)
                .map(e -> (MembershipLowHours) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected MembershipLowHours event"));

        assertEquals(m.getId().value(), low.membershipId());
        assertEquals(TENANT_ID, low.tenantId());
        assertEquals(STUDENT_ID, low.studentId());
        assertEquals(Membership.LOW_HOURS_THRESHOLD, low.remainingHours());
        assertTrue(m.isLowHoursWarningEmitted());
    }

    @Test
    @DisplayName("does not emit a second MembershipLowHours when already below threshold")
    void deduct_alreadyBelowThreshold_doesNotEmitAgain() {
        Membership m = activeWithHours(Membership.LOW_HOURS_THRESHOLD + 1); // 3
        m.deductHours(1, ACTOR_ID, "PROFESSOR"); // → 2, fires once
        m.clearDomainEvents();

        m.deductHours(1, ACTOR_ID, "PROFESSOR"); // → 1, must NOT fire again

        assertEquals(0, lowHoursCount(m));
    }

    @Test
    @DisplayName("deducting straight to zero emits MembershipDepleted but NOT MembershipLowHours")
    void deduct_straightToZero_emitsDepletedNotLowHours() {
        Membership m = activeWithHours(Membership.LOW_HOURS_THRESHOLD + 1); // 3

        m.deductHours(3, ACTOR_ID, "PROFESSOR"); // → 0

        List<DomainEvent> events = m.getDomainEvents();
        assertTrue(events.stream().anyMatch(e -> e instanceof MembershipDepleted),
                "expected MembershipDepleted at zero balance");
        assertEquals(0, lowHoursCount(m), "low-hours warning must not fire at zero — depletion owns that path");
    }

    @Test
    @DisplayName("re-activation re-arms the warning (lowHoursWarningEmitted resets to false)")
    void activate_reArmsLowHoursWarning() {
        Membership m = activeWithHours(Membership.LOW_HOURS_THRESHOLD + 1); // 3
        m.deductHours(1, ACTOR_ID, "PROFESSOR"); // → 2, flag set
        assertTrue(m.isLowHoursWarningEmitted());

        m.deductHours(2, ACTOR_ID, "PROFESSOR"); // → 0, INACTIVE
        m.renew(8, ACTOR_ID);                     // → PENDING_PAYMENT
        m.markProofUploaded();                    // → PENDING_PAYMENT_VALIDATION
        m.validatePayment(ACTOR_ID, false);       // → PENDING_MANAGER_ACTIVATION
        m.activate(UUID.randomUUID());            // → ACTIVE

        assertFalse(m.isLowHoursWarningEmitted(), "activation must re-arm the low-hours warning");
    }

    @Test
    @DisplayName("MembershipDepleted still fires normally when hours reach zero from threshold")
    void deduct_thresholdToZero_emitsDepleted() {
        Membership m = activeWithHours(Membership.LOW_HOURS_THRESHOLD + 1); // 3
        m.deductHours(1, ACTOR_ID, "PROFESSOR"); // → 2 (low-hours)
        m.clearDomainEvents();

        m.deductHours(2, ACTOR_ID, "PROFESSOR"); // → 0

        assertTrue(m.getDomainEvents().stream().anyMatch(e -> e instanceof MembershipDepleted));
    }
}
