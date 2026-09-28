package com.capysoft.tuevento.modules.notification.interfaces.rest;

import com.capysoft.tuevento.modules.notification.application.dto.InAppNotificationPageResponse;
import com.capysoft.tuevento.modules.notification.application.usecase.InAppNotificationQueryUseCase;
import com.capysoft.tuevento.shared.infrastructure.security.SecurityUser;
import com.capysoft.tuevento.shared.interfaces.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints para notificaciones in-app.
 * JWT requerido, siempre limitados al usuario autenticado.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationQueryUseCase inAppNotificationQueryUseCase;

    /**
     * GET /api/v1/notifications/me?page=&size=&unreadOnly=
     * Lista las notificaciones IN_APP del usuario, más recientes primero.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<InAppNotificationPageResponse>> getMyNotifications(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        
        Integer userId = securityUser.getUserId();
        InAppNotificationPageResponse response = inAppNotificationQueryUseCase.listMine(userId, page, size, unreadOnly);
        
        return ResponseEntity.ok(ApiResponse.ok("Notificaciones obtenidas exitosamente", response));
    }

    /**
     * GET /api/v1/notifications/me/unread-count
     * Cuenta las notificaciones no leídas del usuario.
     */
    @GetMapping("/me/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal SecurityUser securityUser) {
        
        Integer userId = securityUser.getUserId();
        long count = inAppNotificationQueryUseCase.unreadCount(userId);
        
        return ResponseEntity.ok(ApiResponse.ok("Cantidad de notificaciones no leídas obtenida exitosamente", count));
    }

    /**
     * PATCH /api/v1/notifications/{notificationUserId}/read
     * Marca una notificación como leída. Solo si pertenece al usuario (si no, 404).
     * Idempotente.
     */
    @PatchMapping("/{notificationUserId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal SecurityUser securityUser,
            @PathVariable Long notificationUserId) {
        
        Integer userId = securityUser.getUserId();
        inAppNotificationQueryUseCase.markRead(userId, notificationUserId);
        
        return ResponseEntity.ok(ApiResponse.ok("Notificación marcada como leída exitosamente"));
    }

    /**
     * PATCH /api/v1/notifications/me/read-all
     * Marca todas las notificaciones no leídas del usuario como leídas.
     */
    @PatchMapping("/me/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal SecurityUser securityUser) {
        
        Integer userId = securityUser.getUserId();
        inAppNotificationQueryUseCase.markAllRead(userId);
        
        return ResponseEntity.ok(ApiResponse.ok("Todas las notificaciones marcadas como leídas exitosamente"));
    }
}