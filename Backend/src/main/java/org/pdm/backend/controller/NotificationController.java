package org.pdm.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.pdm.backend.security.AuthUser;
import org.pdm.backend.service.NotificationService;
import org.pdm.backend.service.NotificationStreamService;
import org.pdm.backend.wrappers.Response;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService notificationStreamService;

    @GetMapping
    @Operation(summary = "Get notifications", description = "Retrieves paginated notifications for the current user")
    @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
    public Response getNotifications(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "newest") String sortBy) {
        return notificationService.getNotifications(authUser.getUser().getId(), page, size, type, sortBy);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", description = "Retrieves the count of unread notifications")
    @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully")
    public Response getUnreadCount(@AuthenticationPrincipal AuthUser authUser) {
        return notificationService.getUnreadCount(authUser.getUser().getId());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream notification events", description = "Opens a server-sent event stream for the current user")
    public SseEmitter stream(@AuthenticationPrincipal AuthUser authUser) {
        return notificationStreamService.subscribe(authUser.getUser().getId());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    public Response markAsRead(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {
        return notificationService.markAsRead(id, authUser.getUser().getId());
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications as read for the current user")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    public Response markAllAsRead(@AuthenticationPrincipal AuthUser authUser) {
        return notificationService.markAllAsRead(authUser.getUser().getId());
    }

    @DeleteMapping("/all")
    @Operation(summary = "Delete all notifications", description = "Deletes all notifications for the current user")
    @ApiResponse(responseCode = "200", description = "All notifications deleted")
    public Response deleteAllNotifications(@AuthenticationPrincipal AuthUser authUser) {
        return notificationService.deleteAllNotifications(authUser.getUser().getId());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification", description = "Deletes a specific notification")
    @ApiResponse(responseCode = "200", description = "Notification deleted successfully")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    public Response deleteNotification(@PathVariable Long id, @AuthenticationPrincipal AuthUser authUser) {
        return notificationService.deleteNotification(id, authUser.getUser().getId());
    }
}
