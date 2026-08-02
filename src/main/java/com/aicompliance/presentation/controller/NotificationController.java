package com.aicompliance.presentation.controller;

import com.aicompliance.application.notification.NotificationDispatchService;
import com.aicompliance.presentation.dto.response.NotificationResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationDispatchService notificationDispatchService;

    public NotificationController(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list() {
        return ResponseEntity.ok(notificationDispatchService.listForCurrentUser().stream()
                .map(NotificationResponse::from)
                .toList());
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<NotificationResponse> acknowledge(@PathVariable UUID id) {
        return ResponseEntity.ok(NotificationResponse.from(notificationDispatchService.acknowledge(id)));
    }
}
