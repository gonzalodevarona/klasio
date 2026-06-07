package com.klasio.auth.application;

import com.klasio.auth.application.dto.AssignRoleCommand;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.service.AssignRoleService;
import com.klasio.auth.domain.event.RoleAssignedEvent;
import com.klasio.auth.domain.exception.RoleElevationForbiddenException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignRoleServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ProfessorRepository professorRepository;

    private AssignRoleService service;

    private static final UUID TENANT_ID       = UUID.randomUUID();
    private static final UUID OTHER_TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AssignRoleService(userRepository, eventPublisher, professorRepository);
    }

    // ── Happy paths ──────────────────────────────────────────────────────────

    @Test
    void superadmin_assignsAdmin_succeeds() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.assign(new AssignRoleCommand(
                targetUserId, Role.ADMIN, UUID.randomUUID(), Role.SUPERADMIN, null));

        assertEquals(Role.ADMIN, targetUser.primaryRole());
        verify(userRepository).save(targetUser);
        verify(eventPublisher).publishEvent(any(RoleAssignedEvent.class));
    }

    @Test
    void admin_assignsManager_sameTenant_succeeds() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", "Target", "User", null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(professorRepository.findById(any(), any())).thenReturn(Optional.empty());
        doNothing().when(professorRepository).save(any(Professor.class));

        service.assign(new AssignRoleCommand(
                targetUserId, Role.MANAGER, UUID.randomUUID(), Role.ADMIN, TENANT_ID));

        assertEquals(Role.MANAGER, targetUser.primaryRole());
        verify(userRepository).save(targetUser);
        verify(professorRepository).save(any(Professor.class));
    }

    @Test
    void admin_assignsProfessor_sameTenant_succeeds() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.assign(new AssignRoleCommand(
                targetUserId, Role.PROFESSOR, UUID.randomUUID(), Role.ADMIN, TENANT_ID));

        assertEquals(Role.PROFESSOR, targetUser.primaryRole());
        assertEquals(Set.of(Role.PROFESSOR), targetUser.getRoles());
    }

    // ── Business rule: assigning MANAGER also grants PROFESSOR ───────────────

    @Test
    void assignManager_alsoGrantsProfessor() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.PROFESSOR,
                IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        // User already has a professor record (was a PROFESSOR before) — no new record created
        when(professorRepository.findById(any(), any())).thenReturn(Optional.of(mock(Professor.class)));

        service.assign(new AssignRoleCommand(
                targetUserId, Role.MANAGER, UUID.randomUUID(), Role.ADMIN, TENANT_ID));

        assertTrue(targetUser.hasRole(Role.MANAGER));
        assertTrue(targetUser.hasRole(Role.PROFESSOR));
        assertEquals(Role.MANAGER, targetUser.primaryRole());
        verify(professorRepository, never()).save(any(Professor.class));
    }

    // ── roleReplaced_notStacked: assigning MANAGER replaces prior role set ────

    @Test
    void roleReplaced_notStacked() {
        UUID targetUserId = UUID.randomUUID();
        // Start as PROFESSOR (single role)
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.PROFESSOR,
                IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        // Already a professor — existing record found, no duplicate created
        when(professorRepository.findById(any(), any())).thenReturn(Optional.of(mock(Professor.class)));

        service.assign(new AssignRoleCommand(
                targetUserId, Role.MANAGER, UUID.randomUUID(), Role.ADMIN, TENANT_ID));

        // MANAGER implies PROFESSOR, so {MANAGER, PROFESSOR} — not stacking old PROFESSOR as third
        assertEquals(Set.of(Role.MANAGER, Role.PROFESSOR), targetUser.getRoles());
        assertFalse(targetUser.hasRole(Role.STUDENT));
        assertFalse(targetUser.hasRole(Role.ADMIN));
    }

    // ── Error paths ──────────────────────────────────────────────────────────

    @Test
    void admin_assignsAdmin_throwsRoleElevationForbidden() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(RoleElevationForbiddenException.class, () ->
                service.assign(new AssignRoleCommand(
                        targetUserId, Role.ADMIN, UUID.randomUUID(), Role.ADMIN, TENANT_ID)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void manager_assignsAnyRole_throwsRoleElevationForbidden() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(RoleElevationForbiddenException.class, () ->
                service.assign(new AssignRoleCommand(
                        targetUserId, Role.PROFESSOR, UUID.randomUUID(), Role.MANAGER, TENANT_ID)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void admin_assignsRole_differentTenant_throwsRoleElevationForbidden() {
        UUID targetUserId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT,
                IdentityDocumentType.CC, "12345678", null, null, null);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(RoleElevationForbiddenException.class, () ->
                service.assign(new AssignRoleCommand(
                        targetUserId, Role.MANAGER, UUID.randomUUID(), Role.ADMIN, OTHER_TENANT_ID)));
        verify(userRepository, never()).save(any());
    }
}
