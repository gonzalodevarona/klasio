package com.klasio.membership.infrastructure.scheduler;

import com.klasio.membership.domain.event.MembershipExpired;
import com.klasio.membership.domain.event.MembershipExpiryWarning;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.program.domain.model.ProgramModality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @DisplayName("onStartup()")
    class OnStartup {

        @Test
        @DisplayName("catches up expired memberships on application startup")
        void onStartup_expiresAllPastDue() {
            Membership m1 = buildActiveMembership();
            Membership m2 = buildInactiveMembership();
            when(membershipRepository.findExpirable(any(LocalDate.class)))
                    .thenReturn(List.of(m1, m2));

            job.onStartup();

            verify(membershipRepository).save(m1);
            verify(membershipRepository).save(m2);
            verify(eventPublisher, times(2)).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("does nothing on startup when no memberships are expirable")
        void onStartup_nothingToDo() {
            when(membershipRepository.findExpirable(any(LocalDate.class)))
                    .thenReturn(List.of());

            job.onStartup();

            verify(membershipRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
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

    // ---- UNLIMITED membership scenarios ----

    @Nested
    @DisplayName("UNLIMITED memberships — expireExpired()")
    class UnlimitedExpireExpired {

        @Test
        @DisplayName("expires an ACTIVE UNLIMITED membership past its expiration date")
        void expireExpired_activeUnlimited_isExpired() {
            Membership m = buildActiveUnlimitedMembership();
            when(membershipRepository.findExpirable(any(LocalDate.class)))
                    .thenReturn(List.of(m));

            job.expireExpired();

            verify(membershipRepository).save(m);
            assertThat(m.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
        }

        @Test
        @DisplayName("publishes MembershipExpired event for an UNLIMITED membership")
        void expireExpired_activeUnlimited_publishesMembershipExpiredEvent() {
            Membership m = buildActiveUnlimitedMembership();
            when(membershipRepository.findExpirable(any(LocalDate.class)))
                    .thenReturn(List.of(m));

            job.expireExpired();

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(MembershipExpired.class);
        }

        @Test
        @DisplayName("expires both UNLIMITED and HOURS_BASED memberships in the same run")
        void expireExpired_mixedModalities_bothExpired() {
            Membership unlimited = buildActiveUnlimitedMembership();
            Membership hoursBased = buildActiveMembership();
            when(membershipRepository.findExpirable(any(LocalDate.class)))
                    .thenReturn(List.of(unlimited, hoursBased));

            job.expireExpired();

            verify(membershipRepository).save(unlimited);
            verify(membershipRepository).save(hoursBased);
            assertThat(unlimited.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
            assertThat(hoursBased.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
            // One MembershipExpired event per membership
            verify(eventPublisher, times(2)).publishEvent(any(Object.class));
        }
    }

    @Nested
    @DisplayName("UNLIMITED memberships — sendExpiryWarnings()")
    class UnlimitedSendExpiryWarnings {

        @Test
        @DisplayName("publishes MembershipExpiryWarning for an UNLIMITED membership expiring in 3 days")
        void sendExpiryWarnings_unlimitedExpiringSoon_publishesWarning() {
            Membership m = buildActiveUnlimitedMembership();
            when(membershipRepository.findExpiringBetween(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(m));

            job.sendExpiryWarnings();

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue()).isInstanceOf(MembershipExpiryWarning.class);
        }

        @Test
        @DisplayName("MembershipExpiryWarning for UNLIMITED carries null remainingHours")
        void sendExpiryWarnings_unlimitedExpiringSoon_warningHasNullRemainingHours() {
            Membership m = buildActiveUnlimitedMembership();
            when(membershipRepository.findExpiringBetween(any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(m));

            job.sendExpiryWarnings();

            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(captor.capture());
            MembershipExpiryWarning warning = (MembershipExpiryWarning) captor.getValue();
            // UNLIMITED memberships have no hour balance — the warning reflects that with null remainingHours
            assertThat(warning.remainingHours()).isNull();
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
                ProgramModality.HOURS_BASED,
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
                ProgramModality.HOURS_BASED,
                10, 0, start, start.withDayOfMonth(start.lengthOfMonth()),
                MembershipStatus.INACTIVE,
                true, java.util.UUID.randomUUID(), java.time.Instant.now(),
                null, null,
                java.time.Instant.now(), java.util.UUID.randomUUID(),
                null, null
        );
    }

    /**
     * Builds an ACTIVE UNLIMITED membership past its expiration date.
     * purchasedHours and availableHours are null (no balance tracking for UNLIMITED).
     */
    private Membership buildActiveUnlimitedMembership() {
        LocalDate start = LocalDate.of(2025, 3, 1);
        LocalDate expiration = LocalDate.of(2025, 3, 31);
        return Membership.reconstitute(
                com.klasio.membership.domain.model.MembershipId.generate(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(), "Unlimited Plan",
                ProgramModality.UNLIMITED,
                null, null, start, expiration,
                MembershipStatus.ACTIVE,
                true, java.util.UUID.randomUUID(), java.time.Instant.now(),
                java.util.UUID.randomUUID(), java.time.Instant.now(),
                java.time.Instant.now(), java.util.UUID.randomUUID(),
                null, null
        );
    }
}
