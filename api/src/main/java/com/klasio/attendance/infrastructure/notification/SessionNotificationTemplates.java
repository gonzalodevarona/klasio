package com.klasio.attendance.infrastructure.notification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class SessionNotificationTemplates {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");

    private SessionNotificationTemplates() {}

    public static String alertTitle(String className, LocalDate date, LocalTime start, LocalTime end) {
        return "Alert on your " + className + " class — "
                + date.format(DATE_FMT) + " " + formatTimeRange(start, end);
    }

    public static String alertBody(String reason) {
        return "Reason: " + reason;
    }

    public static String cancellationTitle(String className, LocalDate date, LocalTime start, LocalTime end) {
        return "Your " + className + " class on "
                + date.format(DATE_FMT) + " " + formatTimeRange(start, end) + " was cancelled";
    }

    public static String cancellationBody(String reason) {
        return "Reason: " + reason + ". No hours were deducted.";
    }

    private static String formatTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) return "";
        return start.format(TIME_FMT) + "–" + end.format(TIME_FMT);
    }
}
