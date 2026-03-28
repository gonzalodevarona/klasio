package com.klasio.membership.application;

import com.klasio.membership.application.dto.DeductHoursCommand;
import com.klasio.membership.application.service.DeductHoursService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeductHoursServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private HourTransactionRepository hourTransactionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DeductHoursService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DeductHoursService(membershipRepository, hourTransactionRepository, eventPublisher);
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("deducts hours and saves transaction")
        void execute_deductsHours_savesTransaction() {
            Membership membership = buildActiveMembership(10);
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membership));

            DeductHoursCommand cmd = new DeductHoursCommand(TENANT_ID, MEMBERSHIP_ID, 3, ACTOR_ID, "PROFESSOR");

            service.execute(cmd);

            verify(hourTransactionRepository).save(any());
            verify(membershipRepository).save(membership);
        }

        @Test
        @DisplayName("sets membership to INACTIVE when hours reach zero")
        void execute_depletes_setsInactive() {
            Membership membership = buildActiveMembership(3);
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.of(membership));

            DeductHoursCommand cmd = new DeductHoursCommand(TENANT_ID, MEMBERSHIP_ID, 3, ACTOR_ID, "PROFESSOR");

            Membership result = service.execute(cmd);

            assert result.getStatus() == MembershipStatus.INACTIVE;
            verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("throws MembershipNotFoundException when membership not found")
        void execute_notFound_throws() {
            when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                    .thenReturn(Optional.empty());

            DeductHoursCommand cmd = new DeductHoursCommand(TENANT_ID, MEMBERSHIP_ID, 1, ACTOR_ID, "PROFESSOR");

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
