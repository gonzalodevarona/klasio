package com.klasio.membership.infrastructure.scheduler;

import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MembershipExpirationJobTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MembershipExpirationJob job;

    @BeforeEach
    void setUp() {
        job = new MembershipExpirationJob(membershipRepository, eventPublisher);
    }

    @Nested
    @DisplayName("expireExpired()")
    class ExpireExpired {

        @Test
        @DisplayName("expires all memberships past their expiration date")
        void expireExpired_expiresAllPastDue() {
            Membership m1 = buildActiveMembership();
            Membership m2 = buildInactiveMembership();
            when(membershipRepository.findExpirable(any(LocalDate.class)))
                    .thenReturn(List.of(m1, m2));

            job.expireExpired();

            verify(membershipRepository).save(m1);
            verify(membershipRepository).save(m2);
            // Two MembershipExpired domain events published (one per membership)
            verify(eventPublisher, times(2)).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("does nothing when no memberships are expirable")
        void expireExpired_nothingToDo() {
            when(membershipRepository.findExpirable(any(LocalDate.class)))
                    .thenReturn(List.of());

            job.expireExpired();

            verify(membershipRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("sendExpiryWarnings()")
    class SendExpiryWarnings {

        @Test
        @DisplayName("publishes expiry warning events for memberships expiring in 3 days")
        void sendExpiryWarnings_publishesWarnings() {
            Membership m = buildActiveMembership();
            when(membershipRepository.findExpiringBetween(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(m));

            job.sendExpiryWarnings();

            verify(eventPublisher, times(1)).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("does nothing when no memberships are expiring soon")
        void sendExpiryWarnings_nothingToDo() {
            when(membershipRepository.findExpiringBetween(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of());

            job.sendExpiryWarnings();

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // ---- Helpers ----

    private Membership buildActiveMembership() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        return Membership.reconstitute(
                com.klasio.membership.domain.model.MembershipId.generate(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(), "Test Plan",
                10, 5, start, start.withDayOfMonth(start.lengthOfMonth()),
                MembershipStatus.ACTIVE,
                true, java.util.UUID.randomUUID(), java.time.Instant.now(),
                null, null,
                java.time.Instant.now(), java.util.UUID.randomUUID(),
                null, null
        );
    }

    private Membership buildInactiveMembership() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        return Membership.reconstitute(
                com.klasio.membership.domain.model.MembershipId.generate(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(), "Test Plan",
                10, 0, start, start.withDayOfMonth(start.lengthOfMonth()),
                MembershipStatus.INACTIVE,
                true, java.util.UUID.randomUUID(), java.time.Instant.now(),
                null, null,
                java.time.Instant.now(), java.util.UUID.randomUUID(),
                null, null
        );
    }
}
