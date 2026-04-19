package com.klasio.attendance;

import java.time.ZoneId;

public final class AttendanceTimeConstants {

    public static final ZoneId TENANT_ZONE = ZoneId.of("America/Bogota");
    public static final int REGISTRATION_CUTOFF_MINUTES = 30;
    public static final int CANCELLATION_CUTOFF_MINUTES = 10;
    public static final int MAX_AVAILABLE_SESSIONS_WINDOW_DAYS = 30;
    public static final int MARKING_WINDOW_MINUTES_BEFORE = 20;
    public static final int MARKING_WINDOW_MINUTES_AFTER  = 10;
    public static final int CORRECTION_WINDOW_HOURS = 24;

    private AttendanceTimeConstants() {}
}
