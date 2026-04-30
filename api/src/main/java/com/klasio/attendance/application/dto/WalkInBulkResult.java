package com.klasio.attendance.application.dto;

import java.util.List;
import java.util.UUID;

public record WalkInBulkResult(
        List<ResultRow> results,
        Summary summary
) {
    public enum Outcome { SUCCESS, FAILED }

    public record ResultRow(
            UUID studentId,
            Outcome outcome,
            UUID registrationId,        // null when FAILED
            String status,              // null when FAILED (e.g. "PRESENT")
            Integer intendedHours,      // null when FAILED
            String errorCode,           // null when SUCCESS
            String errorMessage         // null when SUCCESS
    ) {
        public static ResultRow success(UUID studentId, UUID registrationId, String status, int intendedHours) {
            return new ResultRow(studentId, Outcome.SUCCESS, registrationId, status, intendedHours, null, null);
        }

        public static ResultRow failure(UUID studentId, String errorCode, String errorMessage) {
            return new ResultRow(studentId, Outcome.FAILED, null, null, null, errorCode, errorMessage);
        }
    }

    public record Summary(int total, int succeeded, int failed) {
        public static Summary from(List<ResultRow> rows) {
            int success = (int) rows.stream().filter(r -> r.outcome() == Outcome.SUCCESS).count();
            return new Summary(rows.size(), success, rows.size() - success);
        }
    }
}
