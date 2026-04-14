package com.klasio.membership.domain;

import com.klasio.membership.domain.event.MembershipProofUploaded;
import com.klasio.membership.domain.event.MembershipRenewed;
import com.klasio.membership.domain.model.HourTransactionType;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MembershipTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ENROLLMENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.of(2026, 4, 15);

    private Membership createDefault() {
        return Membership.create(TENANT_ID, STUDENT_ID, ENROLLMENT_ID, PROGRAM_ID,
                PLAN_ID, "Test Plan", 10, START_DATE, ACTOR_ID);
    }

    // ---- create() ----

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates membership in PENDING_PAYMENT with correct fields")
        void create_validInputs_returnsPendingPayment() {
            Membership m = createDefault();

            assertNotNull(m.getId());
            assertEquals(TENANT_ID, m.getTenantId());
            assertEquals(STUDENT_ID, m.getStudentId());
            assertEquals(ENROLLMENT_ID, m.getEnrollmentId());
            assertEquals(PROGRAM_ID, m.getProgramId());
            assertEquals(10, m.getPurchasedHours());
            assertEquals(10, m.getAvailableHours());
            assertEquals(START_DATE, m.getStartDate());
            assertEquals(LocalDate.of(2026, 4, 30), m.getExpirationDate()); // last day of April regardless of start day
            assertEquals(MembershipStatus.PENDING_PAYMENT, m.getStatus());
            assertFalse(m.isPaymentValidated());
            assertEquals(ACTOR_ID, m.getCreatedBy());
            assertNotNull(m.getCreatedAt());
        }

        @Test
        @DisplayName("purchasedHours must be >= 1")
        void create_zeroHours_throwsIllegalArgument() {
            assertThrows(IllegalArgumentException.class, () ->
                    Membership.create(TENANT_ID, STUDENT_ID, ENROLLMENT_ID, PROGRAM_ID,
                            PLAN_ID, "Test Plan", 0, START_DATE, ACTOR_ID));
        }

        @Test
        @DisplayName("emits MembershipCreated event")
        void create_validInputs_emitsMembershipCreatedEvent() {
            Membership m = createDefault();
            List<DomainEvent> events = m.getDomainEvents();
            assertEquals(1, events.size());
        }
    }

    // ---- markProofUploaded() ----

    @Nested
    @DisplayName("markProofUploaded()")
    class MarkProofUploaded {

        @Test
        @DisplayName("transitions PENDING_PAYMENT → PENDING_PAYMENT_VALIDATION")
        void markProofUploaded_pendingPayment_transitionsToPendingValidation() {
            Membership m = createDefault();
            m.clearDomainEvents();

            m.markProofUploaded();

            assertEquals(MembershipStatus.PENDING_PAYMENT_VALIDATION, m.getStatus());
            assertNotNull(m.getUpdatedAt());
        }

        @Test
        @DisplayName("emits MembershipProofUploaded event")
        void markProofUploaded_emitsEvent() {
            Membership m = createDefault();
            m.clearDomainEvents();

            m.markProofUploaded();

            List<DomainEvent> events = m.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(MembershipProofUploaded.class, events.get(0));
        }

        @Test
        @DisplayName("throws when membership is not in PENDING_PAYMENT")
        void markProofUploaded_wrongStatus_throwsIllegalState() {
            Membership m = createDefault();
            m.markProofUploaded(); // now PENDING_PAYMENT_VALIDATION

            assertThrows(IllegalStateException.class, m::markProofUploaded);
        }
    }

    // ---- markProofRejected() ----

    @Nested
    @DisplayName("markProofRejected()")
    class MarkProofRejected {

        @Test
        @DisplayName("transitions PENDING_PAYMENT_VALIDATION → PENDING_PAYMENT")
        void markProofRejected_pendingValidation_transitionsBackToPendingPayment() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.clearDomainEvents();

            m.markProofRejected();

            assertEquals(MembershipStatus.PENDING_PAYMENT, m.getStatus());
            assertNotNull(m.getUpdatedAt());
        }

        @Test
        @DisplayName("throws when membership is not in PENDING_PAYMENT_VALIDATION")
        void markProofRejected_wrongStatus_throwsIllegalState() {
            Membership m = createDefault(); // PENDING_PAYMENT

            assertThrows(IllegalStateException.class, m::markProofRejected);
        }
    }

    // ---- validatePayment() → ACTIVE ----

    @Nested
    @DisplayName("validatePayment() — direct activation")
    class ValidatePaymentDirect {

        @Test
        @DisplayName("transitions PENDING_PAYMENT_VALIDATION → ACTIVE when activateDirectly=true")
        void validatePayment_activateDirectly_transitionsToActive() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.clearDomainEvents();

            m.validatePayment(ACTOR_ID, true);

            assertEquals(MembershipStatus.ACTIVE, m.getStatus());
            assertTrue(m.isPaymentValidated());
            assertEquals(ACTOR_ID, m.getPaymentValidatedBy());
            assertNotNull(m.getPaymentValidatedAt());
            assertEquals(ACTOR_ID, m.getActivatedBy());
            assertNotNull(m.getActivatedAt());
        }

        @Test
        @DisplayName("transitions PENDING_PAYMENT_VALIDATION → PENDING_MANAGER_ACTIVATION when activateDirectly=false")
        void validatePayment_delegate_transitionsToPendingManager() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.clearDomainEvents();

            m.validatePayment(ACTOR_ID, false);

            assertEquals(MembershipStatus.PENDING_MANAGER_ACTIVATION, m.getStatus());
            assertTrue(m.isPaymentValidated());
            assertNull(m.getActivatedBy());
        }

        @Test
        @DisplayName("throws when membership is not in PENDING_PAYMENT_VALIDATION")
        void validatePayment_wrongStatus_throwsInvalidStatusTransition() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true); // now ACTIVE

            assertThrows(IllegalStateException.class, () -> m.validatePayment(ACTOR_ID, true));
        }
    }

    // ---- activate() ----

    @Nested
    @DisplayName("activate()")
    class Activate {

        @Test
        @DisplayName("transitions PENDING_MANAGER_ACTIVATION → ACTIVE")
        void activate_pendingManagerActivation_transitionsToActive() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, false);
            m.clearDomainEvents();

            UUID managerId = UUID.randomUUID();
            m.activate(managerId);

            assertEquals(MembershipStatus.ACTIVE, m.getStatus());
            assertEquals(managerId, m.getActivatedBy());
            assertNotNull(m.getActivatedAt());
        }

        @Test
        @DisplayName("throws when not in PENDING_MANAGER_ACTIVATION")
        void activate_wrongStatus_throwsInvalidStatusTransition() {
            Membership m = createDefault();

            assertThrows(IllegalStateException.class, () -> m.activate(ACTOR_ID));
        }
    }

    // ---- deductHours() ----

    @Nested
    @DisplayName("deductHours()")
    class DeductHours {

        @Test
        @DisplayName("deducts hours and stays ACTIVE when balance > 0")
        void deductHours_partialDeduction_staysActive() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.clearDomainEvents();

            m.deductHours(3, ACTOR_ID, "PROFESSOR");

            assertEquals(7, m.getAvailableHours());
            assertEquals(MembershipStatus.ACTIVE, m.getStatus());
        }

        @Test
        @DisplayName("deducting last hour transitions to INACTIVE")
        void deductHours_lastHour_transitionsToInactive() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.clearDomainEvents();

            m.deductHours(10, ACTOR_ID, "PROFESSOR");

            assertEquals(0, m.getAvailableHours());
            assertEquals(MembershipStatus.INACTIVE, m.getStatus());
        }

        @Test
        @DisplayName("throws when deduction exceeds available hours")
        void deductHours_exceedsBalance_throwsNegativeBalance() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);

            assertThrows(IllegalArgumentException.class, () ->
                    m.deductHours(11, ACTOR_ID, "PROFESSOR"));
        }

        @Test
        @DisplayName("throws when membership is not ACTIVE")
        void deductHours_notActive_throwsIllegalState() {
            Membership m = createDefault();

            assertThrows(IllegalStateException.class, () ->
                    m.deductHours(1, ACTOR_ID, "PROFESSOR"));
        }
    }

    // ---- adjustHours() ----

    @Nested
    @DisplayName("adjustHours()")
    class AdjustHours {

        @Test
        @DisplayName("adds hours to ACTIVE membership")
        void adjustHours_positiveDealt_increasesBalance() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.clearDomainEvents();

            m.adjustHours(5, "Extra hours", ACTOR_ID, "ADMIN");

            assertEquals(15, m.getAvailableHours());
            assertEquals(MembershipStatus.ACTIVE, m.getStatus());
        }

        @Test
        @DisplayName("subtracts hours from ACTIVE membership")
        void adjustHours_negativeDelta_decreasesBalance() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.clearDomainEvents();

            m.adjustHours(-3, "Correction", ACTOR_ID, "ADMIN");

            assertEquals(7, m.getAvailableHours());
            assertEquals(MembershipStatus.ACTIVE, m.getStatus());
        }

        @Test
        @DisplayName("subtraction to zero transitions to INACTIVE")
        void adjustHours_toZero_transitionsToInactive() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.clearDomainEvents();

            m.adjustHours(-10, "Full deduction", ACTOR_ID, "ADMIN");

            assertEquals(0, m.getAvailableHours());
            assertEquals(MembershipStatus.INACTIVE, m.getStatus());
        }

        @Test
        @DisplayName("throws when adjustment would make balance negative")
        void adjustHours_wouldGoNegative_throwsNegativeBalance() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);

            assertThrows(IllegalArgumentException.class, () ->
                    m.adjustHours(-11, "Too much", ACTOR_ID, "ADMIN"));
        }

        @Test
        @DisplayName("throws when delta is zero")
        void adjustHours_zeroDelta_throwsIllegalArgument() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);

            assertThrows(IllegalArgumentException.class, () ->
                    m.adjustHours(0, "Nothing", ACTOR_ID, "ADMIN"));
        }

        @Test
        @DisplayName("throws when membership is not ACTIVE")
        void adjustHours_notActive_throwsIllegalState() {
            Membership m = createDefault();

            assertThrows(IllegalStateException.class, () ->
                    m.adjustHours(5, "Extra", ACTOR_ID, "ADMIN"));
        }
    }

    // ---- expire() ----

    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        @DisplayName("transitions ACTIVE → EXPIRED")
        void expire_activeStatus_transitionsToExpired() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.clearDomainEvents();

            m.expire();

            assertEquals(MembershipStatus.EXPIRED, m.getStatus());
        }

        @Test
        @DisplayName("transitions INACTIVE → EXPIRED (depleted memberships also expire)")
        void expire_inactiveStatus_transitionsToExpired() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.deductHours(10, ACTOR_ID, "PROFESSOR");
            m.clearDomainEvents();

            m.expire();

            assertEquals(MembershipStatus.EXPIRED, m.getStatus());
        }

        @Test
        @DisplayName("throws when already EXPIRED (idempotency guard)")
        void expire_alreadyExpired_throwsIllegalState() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.expire();

            assertThrows(IllegalStateException.class, m::expire);
        }
    }

    // ---- renew() ----

    @Nested
    @DisplayName("renew()")
    class Renew {

        @Test
        @DisplayName("transitions EXPIRED → PENDING_PAYMENT")
        void renew_fromExpired_transitionsToPendingPayment() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.expire();
            m.clearDomainEvents();

            m.renew(8, ACTOR_ID);

            assertEquals(MembershipStatus.PENDING_PAYMENT, m.getStatus());
        }

        @Test
        @DisplayName("transitions INACTIVE → PENDING_PAYMENT")
        void renew_fromInactive_transitionsToPendingPayment() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.deductHours(10, ACTOR_ID, "PROFESSOR"); // depletes → INACTIVE
            m.clearDomainEvents();

            m.renew(8, ACTOR_ID);

            assertEquals(MembershipStatus.PENDING_PAYMENT, m.getStatus());
        }

        @Test
        @DisplayName("throws when membership is ACTIVE")
        void renew_fromActive_throwsIllegalState() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);

            assertThrows(IllegalStateException.class, () -> m.renew(8, ACTOR_ID));
        }

        @Test
        @DisplayName("throws when membership is PENDING_PAYMENT")
        void renew_fromPendingPayment_throwsIllegalState() {
            Membership m = createDefault(); // PENDING_PAYMENT

            assertThrows(IllegalStateException.class, () -> m.renew(8, ACTOR_ID));
        }

        @Test
        @DisplayName("resets hours, clears payment and activation fields")
        void renew_resetsHoursAndClearsPaymentFields() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.expire();
            m.clearDomainEvents();

            m.renew(8, ACTOR_ID);

            assertEquals(8, m.getPurchasedHours());
            assertEquals(8, m.getAvailableHours());
            assertFalse(m.isPaymentValidated());
            assertNull(m.getPaymentValidatedBy());
            assertNull(m.getPaymentValidatedAt());
            assertNull(m.getActivatedBy());
            assertNull(m.getActivatedAt());
        }

        @Test
        @DisplayName("leaves startDate and expirationDate null (set at payment validation)")
        void renew_leavesStartDateNull() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.expire();
            m.clearDomainEvents();

            m.renew(8, ACTOR_ID);

            assertNull(m.getStartDate());
            assertNull(m.getExpirationDate());
        }

        @Test
        @DisplayName("emits MembershipRenewed event")
        void renew_emitsMembershipRenewedEvent() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.expire();
            m.clearDomainEvents();

            m.renew(8, ACTOR_ID);

            List<DomainEvent> events = m.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(MembershipRenewed.class, events.get(0));
            MembershipRenewed event = (MembershipRenewed) events.get(0);
            assertEquals(m.getId().value(), event.membershipId());
            assertEquals(8, event.purchasedHours());
            assertEquals(ACTOR_ID, event.renewedBy());
        }
    }

    // ---- validatePayment() — renewal date logic ----

    @Nested
    @DisplayName("validatePayment() — renewal sets dates")
    class ValidatePaymentRenewal {

        @Test
        @DisplayName("sets startDate and expirationDate when startDate is null (renewal case)")
        void validatePayment_setsStartDateWhenNull() {
            Membership m = createDefault();
            m.markProofUploaded();
            m.validatePayment(ACTOR_ID, true);
            m.expire();
            m.renew(8, ACTOR_ID);
            m.markProofUploaded(); // re-upload after renewal → PENDING_PAYMENT_VALIDATION
            m.clearDomainEvents();

            m.validatePayment(ACTOR_ID, true);

            assertNotNull(m.getStartDate());
            assertNotNull(m.getExpirationDate());
            assertEquals(LocalDate.now(), m.getStartDate());
            assertEquals(
                    LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
                    m.getExpirationDate());
            assertEquals(MembershipStatus.ACTIVE, m.getStatus());
        }
    }
}
