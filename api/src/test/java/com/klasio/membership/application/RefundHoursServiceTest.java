package com.klasio.membership.application;

import com.klasio.membership.application.dto.RefundHoursCommand;
import com.klasio.membership.application.service.RefundHoursService;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.HourTransactionRepository;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundHoursServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private HourTransactionRepository hourTransactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RefundHoursService service;

    private static final UUID TENANT_ID    = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID     = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RefundHoursService(membershipRepository, hourTransactionRepository, eventPublisher);
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("refunds hours to ACTIVE membership and saves transaction")
        void execute_refundsHours_savesTransaction() {
            Membership membership = buildMembership(MembershipStatus.ACTIVE, 8);
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membership));

            service.execute(new RefundHoursCommand(TENANT_ID, MEMBERSHIP_ID, 2, ACTOR_ID, "MANAGER"));

            verify(hourTransactionRepository).save(any());
            verify(membershipRepository).save(membership);
            assertThat(membership.getAvailableHours()).isEqualTo(10);
        }

        @Test
        @DisplayName("refunds hours to INACTIVE membership and transitions back to ACTIVE")
        void execute_inactiveMembership_transitionsToActive() {
            Membership membership = buildMembership(MembershipStatus.INACTIVE, 0);
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membership));

            Membership result = service.execute(
                    new RefundHoursCommand(TENANT_ID, MEMBERSHIP_ID, 3, ACTOR_ID, "MANAGER"));

            assertThat(result.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
            assertThat(result.getAvailableHours()).isEqualTo(3);
        }

        @Test
        @DisplayName("publishes HourAdjusted event")
        void execute_publishesDomainEvent() {
            Membership membership = buildMembership(MembershipStatus.ACTIVE, 5);
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membership));

            service.execute(new RefundHoursCommand(TENANT_ID, MEMBERSHIP_ID, 2, ACTOR_ID, "MANAGER"));

            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("throws MembershipNotFoundException when not found")
        void execute_notFound_throws() {
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.execute(new RefundHoursCommand(TENANT_ID, MEMBERSHIP_ID, 1, ACTOR_ID, "MANAGER")))
                    .isInstanceOf(MembershipNotFoundException.class);
        }
    }

    private Membership buildMembership(MembershipStatus status, int availableHours) {
        LocalDate start = LocalDate.of(2026, 4, 1);
        return Membership.reconstitute(
                MembershipId.of(MEMBERSHIP_ID),
                TENANT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(), "Test Plan",
                10, availableHours,
                start, start.withDayOfMonth(start.lengthOfMonth()),
                status,
                true, UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), Instant.now(),
                Instant.now(), UUID.randomUUID(),
                null, null
        );
    }
}
