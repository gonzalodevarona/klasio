package com.klasio.membership.application;

import com.klasio.membership.application.dto.RenewMembershipCommand;
import com.klasio.membership.application.service.RenewMembershipService;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.ProgramPlanPort;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class RenewMembershipServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private ProgramPlanPort programPlanPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RenewMembershipService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RenewMembershipService(membershipRepository, programPlanPort, eventPublisher);
    }

    private Membership expiredMembership() {
        Membership m = Membership.create(TENANT_ID, UUID.randomUUID(), UUID.randomUUID(),
                PROGRAM_ID, PLAN_ID, "Plan", 10, ProgramModality.HOURS_BASED, LocalDate.of(2026, 4, 1), ACTOR_ID);
        m.markProofUploaded();
        m.validatePayment(ACTOR_ID, true);
        m.expire();
        m.clearDomainEvents();
        return m;
    }

    private ProgramPlanPort.PlanView activePlan() {
        return new ProgramPlanPort.PlanView(
                PLAN_ID, PROGRAM_ID, TENANT_ID, "Plan", "HOURS_BASED", 8, UUID.randomUUID());
    }

    @Test
    @DisplayName("renews expired membership and transitions to PENDING_PAYMENT")
    void renewsExpiredMembership() {
        Membership membership = expiredMembership();

        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
        when(programPlanPort.findActivePlan(membership.getPlanId(), TENANT_ID)).thenReturn(Optional.of(activePlan()));

        RenewMembershipCommand command = new RenewMembershipCommand(TENANT_ID, MEMBERSHIP_ID, ACTOR_ID);
        Membership result = service.execute(command);

        assertThat(result.getStatus()).isEqualTo(MembershipStatus.PENDING_PAYMENT);
        assertThat(result.getPurchasedHours()).isEqualTo(8);
        assertThat(result.getAvailableHours()).isEqualTo(8);
        assertThat(result.getStartDate()).isNull();

        verify(membershipRepository).save(any());
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("throws when membership not found")
    void throwsWhenMembershipNotFound() {
        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.empty());

        RenewMembershipCommand command = new RenewMembershipCommand(TENANT_ID, MEMBERSHIP_ID, ACTOR_ID);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(MembershipNotFoundException.class);
    }

    @Test
    @DisplayName("throws when plan is no longer active")
    void throwsWhenPlanInactive() {
        Membership membership = expiredMembership();

        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
        when(programPlanPort.findActivePlan(membership.getPlanId(), TENANT_ID)).thenReturn(Optional.empty());

        RenewMembershipCommand command = new RenewMembershipCommand(TENANT_ID, MEMBERSHIP_ID, ACTOR_ID);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer active");
    }
}
