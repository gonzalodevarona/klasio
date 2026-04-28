package com.klasio.attendance.infrastructure.notification;

import com.klasio.attendance.domain.event.RegistrationCancelledByLevelChange;
import com.klasio.attendance.domain.port.StudentUserIdPort;
import com.klasio.notifications.application.dto.CreateNotificationCommand;
import com.klasio.notifications.application.port.input.CreateNotificationUseCase;
import com.klasio.notifications.domain.model.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sends one in-app notification to each student whose registration was cancelled
 * because the class level changed from OPEN to a specific level (RF-36).
 *
 * Fires after commit so the notification rows are written in a separate transaction
 * and never block the level-change use case.
 */
@Slf4j
@Component
public class LevelChangeNotificationListener {

    private final CreateNotificationUseCase createNotification;
    private final StudentUserIdPort studentUserIdPort;

    public LevelChangeNotificationListener(CreateNotificationUseCase createNotification,
                                           StudentUserIdPort studentUserIdPort) {
        this.createNotification = createNotification;
        this.studentUserIdPort = studentUserIdPort;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRegistrationCancelledByLevelChange(RegistrationCancelledByLevelChange e) {
        log.info("[NOTIFY] Registration cancelled by level change — registrationId={}, studentId={}, {} -> {}",
                e.registrationId(), e.studentId(), e.previousClassLevel(), e.newClassLevel());

        Optional<UUID> resolvedUserId = studentUserIdPort.findUserIdByStudentId(e.tenantId(), e.studentId());
        if (resolvedUserId.isEmpty()) {
            log.warn("[NOTIFY] No user account found for studentId={} — skipping level-change notification",
                    e.studentId());
            return;
        }

        String title = "Class registration cancelled";
        String body = "Your registration was cancelled because the class level changed"
                + " from " + e.previousClassLevel() + " to " + e.newClassLevel()
                + ". Please register for a class that matches your level.";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("classId", e.classId().toString());
        metadata.put("sessionId", e.sessionId().toString());
        metadata.put("previousClassLevel", e.previousClassLevel());
        metadata.put("newClassLevel", e.newClassLevel());
        metadata.put("deepLink", "/student/registrations");

        createNotification.execute(new CreateNotificationCommand(
                e.tenantId(),
                resolvedUserId.get(),
                NotificationType.CLASS_LEVEL_CHANGED,
                title,
                body,
                metadata,
                e.actorId()));
    }
}
