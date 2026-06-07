package com.klasio.audit.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klasio.audit.domain.model.AuditLogEntry;
import com.klasio.tenant.domain.event.TenantSelfRegistrationToggled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock
    private JpaAuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    private AuditEventListener listener;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        listener = new AuditEventListener(auditLogRepository, objectMapper);
    }

    @Test
    @DisplayName("onTenantSelfRegistrationToggled: writes an audit row with the correct action type")
    void onTenantSelfRegistrationToggled_writesAuditRow() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        listener.onTenantSelfRegistrationToggled(new TenantSelfRegistrationToggled(
                tenantId, "league", false, actorId, Instant.now()));

        verify(auditLogRepository).save(argThat((AuditLogEntry e) ->
                "TENANT_SELF_REGISTRATION_TOGGLED".equals(e.getActionType())
                        && tenantId.equals(e.getTargetEntityId())
                        && actorId.equals(e.getActorId())));
    }
}
