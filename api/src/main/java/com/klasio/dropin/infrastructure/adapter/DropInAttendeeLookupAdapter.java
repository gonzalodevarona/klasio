package com.klasio.dropin.infrastructure.adapter;

import com.klasio.attendance.domain.port.DropInAttendeeLookupPort;
import com.klasio.dropin.infrastructure.persistence.SpringDataDropInAttendeeRepository;
import com.klasio.dropin.infrastructure.persistence.SpringDataDropInPaymentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DropInAttendeeLookupAdapter implements DropInAttendeeLookupPort {

    private final SpringDataDropInAttendeeRepository attendeeRepo;
    private final SpringDataDropInPaymentRepository paymentRepo;

    public DropInAttendeeLookupAdapter(SpringDataDropInAttendeeRepository attendeeRepo,
                                       SpringDataDropInPaymentRepository paymentRepo) {
        this.attendeeRepo = attendeeRepo;
        this.paymentRepo = paymentRepo;
    }

    @Override
    public Map<UUID, DropInRosterEntry> findByAttendeeIds(UUID tenantId, Set<UUID> attendeeIds) {
        if (attendeeIds.isEmpty()) return Map.of();
        return attendeeRepo.findAllById(attendeeIds).stream()
                .filter(e -> tenantId.equals(e.getTenantId()))
                .collect(Collectors.toMap(
                        e -> e.getId(),
                        e -> new DropInRosterEntry(e.getFullName(), e.getPhone())
                ));
    }

    @Override
    public Map<UUID, BigDecimal> findAmountsByPaymentIds(UUID tenantId, Set<UUID> paymentIds) {
        if (paymentIds.isEmpty()) return Map.of();
        return paymentRepo.findAllById(paymentIds).stream()
                .filter(e -> tenantId.equals(e.getTenantId()))
                .collect(Collectors.toMap(
                        e -> e.getId(),
                        e -> e.getAmount()
                ));
    }
}
