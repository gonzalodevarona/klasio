package com.klasio.membership.infrastructure.scheduler;

import com.klasio.membership.domain.event.MembershipExpiryWarning;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Daily cron job that:
 * 1. Expires all ACTIVE/INACTIVE memberships past their expiration date.
 * 2. Sends 3-day expiry warning events for memberships expiring soon.
 */
@Component
public class MembershipExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(MembershipExpirationJob.class);

    private final MembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MembershipExpirationJob(MembershipRepository membershipRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Runs daily at 01:00 UTC.
     * Expires all ACTIVE/INACTIVE memberships whose expiration_date < today.
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    @Transactional
    public void expireExpired() {
        LocalDate today = LocalDate.now();
        List<Membership> expirable = membershipRepository.findExpirable(today);

        log.info("Expiration job: found {} memberships to expire (today={})", expirable.size(), today);

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

    /**
     * Runs daily at 01:05 UTC (after expireExpired).
     * Publishes MembershipExpiryWarning for ACTIVE memberships expiring in exactly 3 days.
     */
    @Scheduled(cron = "0 5 1 * * *", zone = "UTC")
    public void sendExpiryWarnings() {
        LocalDate warningFrom = LocalDate.now().plusDays(3);
        LocalDate warningTo = warningFrom;
        List<Membership> expiringSoon = membershipRepository.findExpiringBetween(warningFrom, warningTo);

        log.info("Expiry warning job: found {} memberships expiring on {}", expiringSoon.size(), warningFrom);

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
}
