package com.klasio.attendance.infrastructure.notification;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class SessionNotificationTemplates {

    private SessionNotificationTemplates() {}

    public static String alertTitle(String className) {
        return "Alert on your " + className + " class";
    }

    public static String alertBody(String reason) {
        return "Reason: " + reason;
    }

    public static String cancellationTitle(String className, LocalDate date) {
        return "Your " + className + " class on " + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + " was cancelled";
    }

    public static String cancellationBody(String reason) {
        return "Reason: " + reason + ". No hours were deducted.";
    }
}
