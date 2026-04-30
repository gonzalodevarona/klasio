package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.RegisterWalkInBulkCommand;
import com.klasio.attendance.application.dto.RegisterWalkInCommand;
import com.klasio.attendance.application.dto.WalkInBulkResult;
import com.klasio.attendance.application.dto.WalkInBulkResult.ResultRow;
import com.klasio.attendance.application.port.input.RegisterWalkInBulkUseCase;
import com.klasio.attendance.application.port.input.RegisterWalkInUseCase;
import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.shared.infrastructure.exception.AlreadyMarkedException;
import com.klasio.shared.infrastructure.exception.ClassLevelMismatchException;
import com.klasio.shared.infrastructure.exception.EnrollmentNotFoundException;
import com.klasio.shared.infrastructure.exception.InsufficientHoursException;
import com.klasio.shared.infrastructure.exception.MarkingWindowException;
import com.klasio.shared.infrastructure.exception.MembershipNotActiveException;
import com.klasio.shared.infrastructure.exception.SessionCancelledException;
import com.klasio.shared.infrastructure.exception.SessionFullException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Processes a batch of walk-in registrations by delegating each student
 * to {@link RegisterWalkInUseCase} in its own independent transaction.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>{@code Propagation.NEVER} on the outer method: the bulk method must not
 *       run inside an outer transaction, so each inner {@code execute()} call
 *       opens and commits its own transaction independently. A failure for one
 *       student never rolls back another student's successful registration.</li>
 *   <li>Known domain exceptions are caught and mapped to error codes; any
 *       unexpected infra exception (NPE, DB outage, etc.) is re-thrown and
 *       surfaces as HTTP 500.</li>
 *   <li>Batch size is capped at 50 to bound database pressure and response
 *       payload size.</li>
 * </ul>
 */
@Service
public class RegisterWalkInBulkService implements RegisterWalkInBulkUseCase {

    private static final int MAX_BATCH = 50;

    private final RegisterWalkInUseCase singleUseCase;

    public RegisterWalkInBulkService(RegisterWalkInUseCase singleUseCase) {
        this.singleUseCase = singleUseCase;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public WalkInBulkResult execute(RegisterWalkInBulkCommand cmd) {
        validateBatch(cmd.studentIds());

        List<ResultRow> rows = new ArrayList<>(cmd.studentIds().size());
        for (UUID studentId : cmd.studentIds()) {
            try {
                RegisterWalkInCommand single = new RegisterWalkInCommand(
                        cmd.tenantId(),
                        cmd.classId(),
                        cmd.sessionDate(),
                        cmd.startTime(),
                        studentId,
                        cmd.hoursToCharge(),
                        cmd.actorUserId(),
                        cmd.actorRole(),
                        cmd.programIdFromJwt());
                AttendanceRegistration r = singleUseCase.execute(single);
                rows.add(ResultRow.success(
                        studentId,
                        r.getId().value(),
                        r.getStatus().name(),
                        r.getIntendedHours()));
            } catch (RuntimeException e) {
                String code = mapToErrorCode(e);
                if (code == null) {
                    // Unexpected infrastructure error — rethrow as HTTP 500
                    throw e;
                }
                rows.add(ResultRow.failure(studentId, code, e.getMessage()));
            }
        }
        return new WalkInBulkResult(rows, WalkInBulkResult.Summary.from(rows));
    }

    private void validateBatch(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new IllegalArgumentException("studentIds must not be empty");
        }
        if (studentIds.size() > MAX_BATCH) {
            throw new IllegalArgumentException("studentIds size must be <= " + MAX_BATCH);
        }
    }

    private String mapToErrorCode(RuntimeException e) {
        if (e instanceof AlreadyMarkedException)       return "ALREADY_MARKED";
        if (e instanceof InsufficientHoursException)   return "INSUFFICIENT_HOURS";
        if (e instanceof MembershipNotActiveException) return "MEMBERSHIP_NOT_ACTIVE";
        if (e instanceof EnrollmentNotFoundException)  return "ENROLLMENT_NOT_FOUND";
        if (e instanceof ClassLevelMismatchException)  return "CLASS_LEVEL_MISMATCH";
        if (e instanceof SessionFullException)         return "SESSION_FULL";
        if (e instanceof SessionCancelledException)    return "SESSION_CANCELLED";
        if (e instanceof MarkingWindowException)       return "MARKING_WINDOW";
        if (e instanceof IllegalArgumentException)     return "INVALID_HOURS";
        return null;
    }
}
