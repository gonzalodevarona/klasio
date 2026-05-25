package com.klasio.dropin.infrastructure.web;

import com.klasio.dropin.application.dto.DropInAttendeeLookupResult;
import com.klasio.dropin.application.service.LookupDropInAttendeeService;
import com.klasio.dropin.infrastructure.web.dto.DropInAttendeeLookupResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/drop-in-attendees")
public class DropInAttendeeLookupController {

    private final LookupDropInAttendeeService service;

    public DropInAttendeeLookupController(LookupDropInAttendeeService service) {
        this.service = service;
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN','MANAGER','PROFESSOR')")
    public ResponseEntity<DropInAttendeeLookupResponse> lookup(
            @RequestParam String phone,
            Authentication auth) {
        @SuppressWarnings("unchecked")
        var details = (Map<String, Object>) auth.getDetails();
        UUID tenantId = UUID.fromString((String) details.get("tenantId"));
        var result = service.lookup(tenantId, phone);
        return ResponseEntity.ok(toResponse(result));
    }

    private DropInAttendeeLookupResponse toResponse(DropInAttendeeLookupResult r) {
        return new DropInAttendeeLookupResponse(
                r.id(), r.fullName(), r.phone(), r.totalVisits(),
                r.firstVisitAt(), r.lastVisitAt(), r.converted());
    }
}
