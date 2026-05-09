package com.klasio.dropin.application.service;

import com.klasio.dropin.application.dto.DropInAttendeeLookupResult;
import com.klasio.dropin.domain.model.DropInAttendee;
import com.klasio.dropin.domain.model.DropInAttendeeId;
import com.klasio.dropin.domain.port.DropInAttendeeRepository;
import com.klasio.shared.infrastructure.exception.DropInAttendeeNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LookupDropInAttendeeServiceTest {

    @Mock DropInAttendeeRepository repo;
    @InjectMocks LookupDropInAttendeeService service;

    @Test
    void lookup_found_returnsResult() {
        UUID tenantId = UUID.randomUUID();
        String phone = "3001234567";
        DropInAttendee attendee = DropInAttendee.create(tenantId, "Ana García", phone, UUID.randomUUID(), Instant.now());
        when(repo.findByPhoneAndTenant(phone, tenantId)).thenReturn(Optional.of(attendee));

        DropInAttendeeLookupResult result = service.lookup(tenantId, phone);

        assertThat(result.fullName()).isEqualTo("Ana García");
        assertThat(result.phone()).isEqualTo(phone);
        assertThat(result.converted()).isFalse();
    }

    @Test
    void lookup_notFound_throwsException() {
        when(repo.findByPhoneAndTenant(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lookup(UUID.randomUUID(), "3001234567"))
                .isInstanceOf(DropInAttendeeNotFoundException.class);
    }
}
