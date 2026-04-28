package com.klasio.membership.application;

import com.klasio.membership.application.dto.ActivateMembershipCommand;
import com.klasio.membership.application.service.ActivateMembershipService;
import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.shared.infrastructure.exception.ManagerProgramMismatchException;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateMembershipServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ActivateMembershipService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ActivateMembershipService(membershipRepository, eventPublisher);
    }

    private Membership pendingManagerMembership() {
        Membership m = Membership.create(TENANT_ID, UUID.randomUUID(), UUID.randomUUID(), PROGRAM_ID,
                UUID.randomUUID(), "Test Plan", 10, ProgramModality.HOURS_BASED, LocalDate.of(2026, 4, 1), ADMIN_ID);
        m.markProofUploaded(); // PENDING_PAYMENT → PENDING_PAYMENT_VALIDATION
        m.validatePayment(ADMIN_ID, false); // → PENDING_MANAGER_ACTIVATION
        m.clearDomainEvents();
        return m;
    }

    @Test
    @DisplayName("admin activates any PENDING_MANAGER_ACTIVATION membership")
    void execute_adminActivates_transitionsToActive() {
        Membership m = pendingManagerMembership();
        UUID id = m.getId().value();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.of(m));

        // null programId = admin/superadmin (no scope restriction)
        service.execute(new ActivateMembershipCommand(TENANT_ID, id, ADMIN_ID, "ADMIN", null));

        assertThat(m.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        verify(membershipRepository).save(m);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e instanceof MembershipActivated);
    }

    @Test
    @DisplayName("manager activates when programId matches")
    void execute_managerActivates_ownProgram_succeeds() {
        Membership m = pendingManagerMembership();
        UUID id = m.getId().value();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.of(m));

        service.execute(new ActivateMembershipCommand(TENANT_ID, id, MANAGER_ID, "MANAGER", PROGRAM_ID));

        assertThat(m.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("manager gets ManagerProgramMismatchException when programId doesn't match")
    void execute_managerActivates_wrongProgram_throwsMismatch() {
        Membership m = pendingManagerMembership();
        UUID id = m.getId().value();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.of(m));

        UUID wrongProgramId = UUID.randomUUID();
        assertThatThrownBy(() ->
                service.execute(new ActivateMembershipCommand(TENANT_ID, id, MANAGER_ID, "MANAGER", wrongProgramId)))
                .isInstanceOf(ManagerProgramMismatchException.class);

        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws MembershipNotFoundException when id not found")
    void execute_notFound_throwsMembershipNotFound() {
        UUID id = UUID.randomUUID();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(new ActivateMembershipCommand(TENANT_ID, id, ADMIN_ID, "ADMIN", null)))
                .isInstanceOf(MembershipNotFoundException.class);
    }

    @Test
    @DisplayName("throws when not in PENDING_MANAGER_ACTIVATION status")
    void execute_wrongStatus_throwsIllegalState() {
        Membership m = Membership.create(TENANT_ID, UUID.randomUUID(), UUID.randomUUID(), PROGRAM_ID,
                UUID.randomUUID(), "Test Plan", 10, ProgramModality.HOURS_BASED, LocalDate.of(2026, 4, 1), ADMIN_ID);
        // still PENDING_PAYMENT (no proof uploaded yet)
        UUID id = m.getId().value();
        when(membershipRepository.findById(TENANT_ID, id)).thenReturn(Optional.of(m));

        assertThatThrownBy(() ->
                service.execute(new ActivateMembershipCommand(TENANT_ID, id, ADMIN_ID, "ADMIN", null)))
                .isInstanceOf(IllegalStateException.class);
    }
}
