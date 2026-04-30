package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.dto.WalkInBulkResult.Outcome;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterWalkInBulkServiceTest {

    @Mock RegisterWalkInUseCase singleUseCase;
    @InjectMocks RegisterWalkInBulkService service;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID CLASS_ID   = UUID.randomUUID();
    private static final UUID ACTOR_ID   = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final LocalDate SESSION_DATE = LocalDate.of(2026, 4, 29);
    private static final LocalTime START = LocalTime.of(18, 0);

    private RegisterWalkInBulkCommand cmd(List<UUID> studentIds, int hours) {
        return new RegisterWalkInBulkCommand(
                TENANT_ID, CLASS_ID, SESSION_DATE, START,
                studentIds, hours, ACTOR_ID, "ADMIN", PROGRAM_ID);
    }

    // Helper: create a mock AttendanceRegistration that returns what we need.
    // AttendanceRegistrationId is a record — its value() accessor is final and cannot
    // be stubbed with Mockito. Instantiate it directly and stub reg.getId() normally.
    private com.klasio.attendance.domain.model.AttendanceRegistration fakeReg(UUID regId) {
        com.klasio.attendance.domain.model.AttendanceRegistration reg =
                org.mockito.Mockito.mock(com.klasio.attendance.domain.model.AttendanceRegistration.class);
        com.klasio.attendance.domain.model.AttendanceRegistrationId rid =
                new com.klasio.attendance.domain.model.AttendanceRegistrationId(regId);
        when(reg.getId()).thenReturn(rid);
        when(reg.getStatus()).thenReturn(com.klasio.attendance.domain.model.AttendanceRegistrationStatus.PRESENT);
        when(reg.getIntendedHours()).thenReturn(1);
        return reg;
    }

    @Test
    void allSucceed_returnsAllSuccessRows() {
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID();
        // Build fakes before stubbing to avoid Mockito's UnfinishedStubbingException
        // caused by calling when() inside a when().thenReturn() argument.
        var r1 = fakeReg(UUID.randomUUID());
        var r2 = fakeReg(UUID.randomUUID());
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenReturn(r1)
                .thenReturn(r2);

        WalkInBulkResult result = service.execute(cmd(List.of(s1, s2), 1));

        assertThat(result.summary().total()).isEqualTo(2);
        assertThat(result.summary().succeeded()).isEqualTo(2);
        assertThat(result.summary().failed()).isZero();
        assertThat(result.results()).extracting(WalkInBulkResult.ResultRow::outcome)
                .containsOnly(Outcome.SUCCESS);
    }

    @Test
    void partialFailure_returnsMixedRows() {
        UUID s1 = UUID.randomUUID(), s2 = UUID.randomUUID(), s3 = UUID.randomUUID();
        // Build fakes before stubbing to avoid Mockito's UnfinishedStubbingException.
        var r1 = fakeReg(UUID.randomUUID());
        var r3 = fakeReg(UUID.randomUUID());
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenReturn(r1)
                .thenThrow(new InsufficientHoursException("not enough"))
                .thenReturn(r3);

        WalkInBulkResult result = service.execute(cmd(List.of(s1, s2, s3), 1));

        assertThat(result.summary().succeeded()).isEqualTo(2);
        assertThat(result.summary().failed()).isEqualTo(1);
        assertThat(result.results().get(1).outcome()).isEqualTo(Outcome.FAILED);
        assertThat(result.results().get(1).errorCode()).isEqualTo("INSUFFICIENT_HOURS");
    }

    @Test
    void emptyStudentIds_throws() {
        assertThatThrownBy(() -> service.execute(cmd(List.of(), 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void overSizeLimit_throws() {
        List<UUID> tooMany = new ArrayList<>();
        for (int i = 0; i < 51; i++) tooMany.add(UUID.randomUUID());
        assertThatThrownBy(() -> service.execute(cmd(tooMany, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void alreadyMarkedException_mapsToErrorCode() {
        UUID s1 = UUID.randomUUID();
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenThrow(new AlreadyMarkedException("already"));
        WalkInBulkResult result = service.execute(cmd(List.of(s1), 1));
        assertThat(result.results().get(0).errorCode()).isEqualTo("ALREADY_MARKED");
    }

    @Test
    void sessionFullException_mapsToErrorCode() {
        UUID s1 = UUID.randomUUID();
        when(singleUseCase.execute(any(RegisterWalkInCommand.class)))
                .thenThrow(new SessionFullException("full"));
        WalkInBulkResult result = service.execute(cmd(List.of(s1), 1));
        assertThat(result.results().get(0).errorCode()).isEqualTo("SESSION_FULL");
    }
}
