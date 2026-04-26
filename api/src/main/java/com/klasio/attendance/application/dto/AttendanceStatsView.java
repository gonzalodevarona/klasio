package com.klasio.attendance.application.dto;

public record AttendanceStatsView(
        long attended,
        long cancelledByStudent,
        long cancelledBySystem,
        long absent,
        long totalHoursConsumed,
        int attendanceRatePercent
) {}
