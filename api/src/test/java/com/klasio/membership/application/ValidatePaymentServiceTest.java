package com.klasio.membership.application;

import com.klasio.membership.application.dto.ValidatePaymentCommand;
import com.klasio.membership.application.service.ValidatePaymentService;
import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.event.MembershipPendingManagerActivation;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidatePaymentServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ValidatePaymentService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ValidatePaymentService(membershipRepository, eventPublisher);
    }

    private Membership pendingMembership() {
        return Membership.create(TENANT_ID, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Test Plan", 10, LocalDate.of(2026, 4, 1), UUID.randomUUID());
    }

    @Test
    @DisplayName("activateDirectly=true transitions to ACTIVE and publishes MembershipActivated")
    void execute_activateDirectly_transitionsToActive() {
        Membership m = pendingMembership();
        UUID id = m.getId().value();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.of(m));

        service.execute(new ValidatePaymentCommand(TENANT_ID, id, true, ACTOR_ID));

        assertThat(m.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        verify(membershipRepository).save(m);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof MembershipActivated);
    }

    @Test
    @DisplayName("activateDirectly=false transitions to PENDING_MANAGER_ACTIVATION")
    void execute_delegate_transitionsToPendingManager() {
        Membership m = pendingMembership();
        UUID id = m.getId().value();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.of(m));

        service.execute(new ValidatePaymentCommand(TENANT_ID, id, false, ACTOR_ID));

        assertThat(m.getStatus()).isEqualTo(MembershipStatus.PENDING_MANAGER_ACTIVATION);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof MembershipPendingManagerActivation);
    }

    @Test
    @DisplayName("throws MembershipNotFoundException when id not found")
    void execute_notFound_throwsMembershipNotFound() {
        UUID id = UUID.randomUUID();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(new ValidatePaymentCommand(TENANT_ID, id, true, ACTOR_ID)))
                .isInstanceOf(MembershipNotFoundException.class);
    }

    @Test
    @DisplayName("throws when membership not in PENDING_PAYMENT_VALIDATION")
    void execute_wrongStatus_throwsIllegalState() {
        Membership m = pendingMembership();
        m.validatePayment(ACTOR_ID, true); // already ACTIVE
        UUID id = m.getId().value();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.of(m));

        assertThatThrownBy(() ->
                service.execute(new ValidatePaymentCommand(TENANT_ID, id, true, ACTOR_ID)))
                .isInstanceOf(IllegalStateException.class);

        verify(membershipRepository, never()).save(any());
    }
}
