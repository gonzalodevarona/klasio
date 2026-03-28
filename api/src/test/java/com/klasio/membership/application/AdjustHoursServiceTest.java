package com.klasio.membership.application;

import com.klasio.membership.application.dto.AdjustHoursCommand;
import com.klasio.membership.application.service.AdjustHoursService;
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
class AdjustHoursServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private HourTransactionRepository hourTransactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AdjustHoursService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AdjustHoursService(membershipRepository, hourTransactionRepository, eventPublisher);
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("adds hours and saves transaction")
        void execute_addsHours_savesTransaction() {
            Membership membership = buildActiveMembership(5);
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membership));

            AdjustHoursCommand cmd = new AdjustHoursCommand(
                    TENANT_ID, MEMBERSHIP_ID, 3, "Adding hours for extra session", ACTOR_ID, "ADMIN");

            Membership result = service.execute(cmd);

            assertThat(result.getAvailableHours()).isEqualTo(8);
            verify(hourTransactionRepository).save(any());
            verify(membershipRepository).save(membership);
        }

        @Test
        @DisplayName("subtracts hours and saves transaction")
        void execute_subtractsHours_savesTransaction() {
            Membership membership = buildActiveMembership(5);
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membership));

            AdjustHoursCommand cmd = new AdjustHoursCommand(
                    TENANT_ID, MEMBERSHIP_ID, -2, "Correction for billing error", ACTOR_ID, "ADMIN");

            Membership result = service.execute(cmd);

            assertThat(result.getAvailableHours()).isEqualTo(3);
            verify(hourTransactionRepository).save(any());
        }

        @Test
        @DisplayName("throws when reason is too short")
        void execute_shortReason_throws() {
            AdjustHoursCommand cmd = new AdjustHoursCommand(
                    TENANT_ID, MEMBERSHIP_ID, 3, "bad", ACTOR_ID, "ADMIN");

            assertThatThrownBy(() -> service.execute(cmd))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws MembershipNotFoundException when membership not found")
        void execute_notFound_throws() {
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.empty());

            AdjustHoursCommand cmd = new AdjustHoursCommand(
                    TENANT_ID, MEMBERSHIP_ID, 3, "Adding hours for valid reason", ACTOR_ID, "ADMIN");

            assertThatThrownBy(() -> service.execute(cmd))
                    .isInstanceOf(MembershipNotFoundException.class);
        }
    }

    private Membership buildActiveMembership(int availableHours) {
        LocalDate start = LocalDate.of(2026, 3, 1);
        return Membership.reconstitute(
                MembershipId.of(MEMBERSHIP_ID),
                TENANT_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(), "Test Plan",
                10, availableHours,
                start, start.withDayOfMonth(start.lengthOfMonth()),
                MembershipStatus.ACTIVE,
                true, UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), Instant.now(),
                Instant.now(), UUID.randomUUID(),
                null, null
        );
    }
}
