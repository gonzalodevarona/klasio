package com.klasio.dropin.application.service;

import com.klasio.dropin.application.dto.DropInAttendeeLookupResult;
import com.klasio.dropin.domain.port.DropInAttendeeRepository;
import com.klasio.shared.infrastructure.exception.DropInAttendeeNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LookupDropInAttendeeService {

    private final DropInAttendeeRepository repo;

    public LookupDropInAttendeeService(DropInAttendeeRepository repo) {
        this.repo = repo;
    }

    public DropInAttendeeLookupResult lookup(UUID tenantId, String phone) {
        var attendee = repo.findByPhoneAndTenant(phone, tenantId)
                .orElseThrow(() -> new DropInAttendeeNotFoundException("No attendee with phone " + phone));
        return new DropInAttendeeLookupResult(
                attendee.getId().value(),
                attendee.getFullName(),
                attendee.getPhone(),
                attendee.getTotalVisits(),
                attendee.getFirstVisitAt(),
                attendee.getLastVisitAt(),
                attendee.getConvertedToStudentId() != null);
    }
}
