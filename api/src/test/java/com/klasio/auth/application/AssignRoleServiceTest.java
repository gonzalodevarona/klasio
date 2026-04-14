package com.klasio.auth.application;

import com.klasio.auth.application.dto.AssignRoleCommand;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.application.service.AssignRoleService;
import com.klasio.auth.domain.event.RoleAssignedEvent;
import com.klasio.auth.domain.exception.RoleElevationForbiddenException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignRoleServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AssignRoleService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID OTHER_TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AssignRoleService(userRepository, eventPublisher);
    }

    @Test
    void superadmin_assignsAdmin_succeeds() {
        UUID targetUserId = UUID.randomUUID();
        UUID assignerId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignRoleCommand command = new AssignRoleCommand(
                targetUserId, Role.ADMIN, assignerId, Role.SUPERADMIN, null);

        service.assign(command);

        assertEquals(Role.ADMIN, targetUser.getRole());
        verify(userRepository).save(targetUser);
        verify(eventPublisher).publishEvent(any(RoleAssignedEvent.class));
    }

    @Test
    void admin_assignsManager_sameTenant_succeeds() {
        UUID targetUserId = UUID.randomUUID();
        UUID assignerId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignRoleCommand command = new AssignRoleCommand(
                targetUserId, Role.MANAGER, assignerId, Role.ADMIN, TENANT_ID);

        service.assign(command);

        assertEquals(Role.MANAGER, targetUser.getRole());
        verify(userRepository).save(targetUser);
    }

    @Test
    void admin_assignsProfessor_sameTenant_succeeds() {
        UUID targetUserId = UUID.randomUUID();
        UUID assignerId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignRoleCommand command = new AssignRoleCommand(
                targetUserId, Role.PROFESSOR, assignerId, Role.ADMIN, TENANT_ID);

        service.assign(command);

        assertEquals(Role.PROFESSOR, targetUser.getRole());
    }

    @Test
    void admin_assignsAdmin_throwsRoleElevationForbidden() {
        UUID targetUserId = UUID.randomUUID();
        UUID assignerId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        AssignRoleCommand command = new AssignRoleCommand(
                targetUserId, Role.ADMIN, assignerId, Role.ADMIN, TENANT_ID);

        assertThrows(RoleElevationForbiddenException.class, () -> service.assign(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    void manager_assignsAnyRole_throwsRoleElevationForbidden() {
        UUID targetUserId = UUID.randomUUID();
        UUID assignerId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        AssignRoleCommand command = new AssignRoleCommand(
                targetUserId, Role.PROFESSOR, assignerId, Role.MANAGER, TENANT_ID);

        assertThrows(RoleElevationForbiddenException.class, () -> service.assign(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    void admin_assignsRole_differentTenant_throwsRoleElevationForbidden() {
        UUID targetUserId = UUID.randomUUID();
        UUID assignerId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.STUDENT, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        AssignRoleCommand command = new AssignRoleCommand(
                targetUserId, Role.MANAGER, assignerId, Role.ADMIN, OTHER_TENANT_ID);

        assertThrows(RoleElevationForbiddenException.class, () -> service.assign(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    void roleReplaced_notStacked() {
        UUID targetUserId = UUID.randomUUID();
        UUID assignerId = UUID.randomUUID();
        User targetUser = User.createActive(TENANT_ID, "target@example.com", "hash", Role.PROFESSOR, com.klasio.shared.domain.model.IdentityDocumentType.CC, "12345678");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignRoleCommand command = new AssignRoleCommand(
                targetUserId, Role.MANAGER, assignerId, Role.ADMIN, TENANT_ID);

        service.assign(command);

        assertEquals(Role.MANAGER, targetUser.getRole());
    }
}
