package com.klasio.dropin.domain.model;

import com.klasio.dropin.domain.event.DropInPaymentRecorded;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DropInPaymentTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ATTENDEE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("25000.00");
    private static final BigDecimal PROGRAM_PRICE = new BigDecimal("20000.00");

    private DropInPayment createDefault() {
        return DropInPayment.create(
                TENANT_ID, ATTENDEE_ID, SESSION_ID, PROGRAM_ID,
                VALID_AMOUNT, PaymentMethod.CASH, PROGRAM_PRICE,
                ACTOR_ID, Instant.now()
        );
    }

    @Test
    void create_rejectsZeroAmount() {
        assertThatThrownBy(() -> DropInPayment.create(
                TENANT_ID, ATTENDEE_ID, SESSION_ID, PROGRAM_ID,
                BigDecimal.ZERO, PaymentMethod.CASH, PROGRAM_PRICE,
                ACTOR_ID, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsNegativeAmount() {
        assertThatThrownBy(() -> DropInPayment.create(
                TENANT_ID, ATTENDEE_ID, SESSION_ID, PROGRAM_ID,
                new BigDecimal("-1.00"), PaymentMethod.CASH, PROGRAM_PRICE,
                ACTOR_ID, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejectsNullPaymentMethod() {
        assertThatThrownBy(() -> DropInPayment.create(
                TENANT_ID, ATTENDEE_ID, SESSION_ID, PROGRAM_ID,
                VALID_AMOUNT, null, PROGRAM_PRICE,
                ACTOR_ID, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_happyPath_setsAllFields() {
        DropInPayment payment = createDefault();

        assertThat(payment.getId()).isNotNull();
        assertThat(payment.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(payment.getAttendeeId()).isEqualTo(ATTENDEE_ID);
        assertThat(payment.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(payment.getProgramId()).isEqualTo(PROGRAM_ID);
        assertThat(payment.getAmount()).isEqualByComparingTo(VALID_AMOUNT);
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(payment.getProgramDropInPrice()).isEqualByComparingTo(PROGRAM_PRICE);
        assertThat(payment.getActorId()).isEqualTo(ACTOR_ID);
        assertThat(payment.getCreatedAt()).isNotNull();
    }

    @Test
    void create_emitsDomainEvent() {
        DropInPayment payment = createDefault();

        assertThat(payment.getDomainEvents()).hasSize(1);
        assertThat(payment.getDomainEvents().get(0)).isInstanceOf(DropInPaymentRecorded.class);

        DropInPaymentRecorded event = (DropInPaymentRecorded) payment.getDomainEvents().get(0);
        assertThat(event.attendeeId()).isEqualTo(ATTENDEE_ID);
        assertThat(event.sessionId()).isEqualTo(SESSION_ID);
        assertThat(event.amount()).isEqualByComparingTo(VALID_AMOUNT);
        assertThat(event.paymentMethod()).isEqualTo(PaymentMethod.CASH);
    }
}
