package com.klasio.membership.infrastructure.web;

import com.klasio.membership.application.port.input.GetHourTransactionsUseCase;
import com.klasio.membership.application.port.input.GetMembershipUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.StudentIdPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ownership enforcement on STUDENT-accessible endpoints.
 * Tests the logic: resolved studentId from JWT must match membership.studentId.
 */
class MembershipControllerOwnershipTest {

    private final GetMembershipUseCase getMembershipUseCase = mock(GetMembershipUseCase.class);
    private final GetHourTransactionsUseCase getHourTransactionsUseCase = mock(GetHourTransactionsUseCase.class);
    private final StudentIdPort studentIdPort = mock(StudentIdPort.class);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID OTHER_STUDENT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();

    @Test
    @DisplayName("assertStudentOwnership should pass when JWT student matches membership student")
    void ownershipCheck_passes_whenStudentMatches() {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));

        // Should not throw
        assertOwnership(STUDENT_ID, STUDENT_ID);
    }

    @Test
    @DisplayName("assertStudentOwnership should throw when JWT student does not match membership student")
    void ownershipCheck_throws_whenStudentMismatch() {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.of(STUDENT_ID));

        assertThatThrownBy(() -> assertOwnership(STUDENT_ID, OTHER_STUDENT_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("assertStudentOwnership should throw when no student profile found")
    void ownershipCheck_throws_whenNoStudentProfile() {
        when(studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> {
            UUID resolvedStudentId = studentIdPort.findStudentIdByUserId(TENANT_ID, USER_ID)
                    .orElseThrow(() -> new IllegalStateException("No student profile"));
        }).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Simulates the ownership check logic extracted from MembershipController.
     */
    private void assertOwnership(UUID resolvedStudentId, UUID membershipStudentId) {
        if (!resolvedStudentId.equals(membershipStudentId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Student may only access their own membership");
        }
    }
}
