package com.klasio.membership.infrastructure.scheduler;

import com.klasio.membership.domain.event.MembershipExpiryWarning;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Daily cron job that:
 * 1. Expires all ACTIVE/INACTIVE memberships past their expiration date.
 * 2. Sends 3-day expiry warning events for memberships expiring soon.
 */
@Slf4j
@Component
public class MembershipExpirationJob {

    private final MembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MembershipExpirationJob(MembershipRepository membershipRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Catches up on any missed expirations when the application starts.
     * Spring @Scheduled does not execute missed cron triggers, so this ensures
     * memberships that should have expired while the app was down are processed immediately.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        log.info("Application started — running catch-up expiration check");
        runExpiration();
    }

    /**
     * Runs daily at 01:00 UTC.
     * Expires all ACTIVE/INACTIVE memberships whose expiration_date < today (UTC).
     */
    @Scheduled(cron = "0 30 4 * * *", zone = "UTC")
    @Transactional
    public void expireExpired() {
        runExpiration();
    }

    /**
     * Runs daily at 01:05 UTC (after expireExpired).
     * Publishes MembershipExpiryWarning for ACTIVE memberships expiring in exactly 3 days.
     */
    @Scheduled(cron = "0 30 14 * * *", zone = "UTC")
    public void sendExpiryWarnings() {
        LocalDate warningDate = LocalDate.now(ZoneOffset.UTC).plusDays(3);
        List<Membership> expiringSoon = membershipRepository.findExpiringBetween(warningDate, warningDate);

        log.info("Expiry warning job: found {} memberships expiring on {}", expiringSoon.size(), warningDate);

        for (Membership membership : expiringSoon) {
            MembershipExpiryWarning warning = new MembershipExpiryWarning(
                    membership.getId().value(),
                    membership.getTenantId(),
                    membership.getStudentId(),
                    membership.getProgramId(),
                    membership.getExpirationDate(),
                    Instant.now()
            );
            eventPublisher.publishEvent(warning);
        }
    }

    private void runExpiration() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Membership> expirable = membershipRepository.findExpirable(today);

        log.info("Expiration check: found {} memberships to expire (today={})", expirable.size(), today);

        for (Membership membership : expirable) {
            try {
                membership.expire();
                membershipRepository.save(membership);
                membership.getDomainEvents().forEach(eventPublisher::publishEvent);
                membership.clearDomainEvents();
            } catch (IllegalStateException ex) {
                log.warn("Could not expire membership {}: {}", membership.getId().value(), ex.getMessage());
            }
        }
    }
}
