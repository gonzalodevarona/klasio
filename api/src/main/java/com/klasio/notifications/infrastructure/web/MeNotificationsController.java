package com.klasio.notifications.infrastructure.web;

import com.klasio.notifications.application.port.input.GetUnreadCountUseCase;
import com.klasio.notifications.application.port.input.ListMyNotificationsUseCase;
import com.klasio.notifications.application.port.input.MarkAllNotificationsReadUseCase;
import com.klasio.notifications.application.port.input.MarkNotificationReadUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/notifications")
@PreAuthorize("isAuthenticated()")
public class MeNotificationsController {

    private final ListMyNotificationsUseCase listUseCase;
    private final GetUnreadCountUseCase unreadCountUseCase;
    private final MarkNotificationReadUseCase markReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllReadUseCase;

    public MeNotificationsController(ListMyNotificationsUseCase listUseCase,
                                     GetUnreadCountUseCase unreadCountUseCase,
                                     MarkNotificationReadUseCase markReadUseCase,
                                     MarkAllNotificationsReadUseCase markAllReadUseCase) {
        this.listUseCase = listUseCase;
        this.unreadCountUseCase = unreadCountUseCase;
        this.markReadUseCase = markReadUseCase;
        this.markAllReadUseCase = markAllReadUseCase;
    }

    @GetMapping
    public NotificationDtos.NotificationPageResponse list(
            Authentication auth,
            @RequestParam(required = false, defaultValue = "false") boolean unread,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        UUID tenantId = extractTenantId(auth);
        UUID userId = extractUserId(auth);
        ListMyNotificationsUseCase.Result r = listUseCase.execute(tenantId, userId, unread, page, size);
        return new NotificationDtos.NotificationPageResponse(
                r.items().stream().map(NotificationDtos.NotificationResponse::from).toList(),
                r.total(), r.page(), r.size());
    }

    @GetMapping("/unread-count")
    public NotificationDtos.UnreadCountResponse unreadCount(Authentication auth) {
        long count = unreadCountUseCase.execute(extractTenantId(auth), extractUserId(auth));
        return new NotificationDtos.UnreadCountResponse(count);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Authentication auth, @PathVariable UUID id) {
        markReadUseCase.execute(extractTenantId(auth), extractUserId(auth), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        markAllReadUseCase.execute(extractTenantId(auth), extractUserId(auth));
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private static UUID extractTenantId(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        return UUID.fromString(claims.get("tenantId").toString());
    }

    @SuppressWarnings("unchecked")
    private static UUID extractUserId(Authentication auth) {
        Map<String, Object> claims = (Map<String, Object>) auth.getDetails();
        return UUID.fromString(claims.get("userId").toString());
    }
}
