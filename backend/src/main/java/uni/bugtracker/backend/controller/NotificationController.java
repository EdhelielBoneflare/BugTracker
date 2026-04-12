package uni.bugtracker.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uni.bugtracker.backend.dto.notification.NotificationSetupRequest;
import uni.bugtracker.backend.dto.notification.NotificationSetupResponse;
import uni.bugtracker.backend.dto.notification.NotificationStateResponse;
import uni.bugtracker.backend.model.Report;
import uni.bugtracker.backend.service.NotificationService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/api/notifications/states")
    public ResponseEntity<List<NotificationStateResponse>> getNotificationStates(@RequestParam String projectId) {
        return ResponseEntity.ok().body(notificationService.getNotificationStates(projectId));
    }

    @PostMapping("/api/notifications/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationSetupResponse> addDevNotification(@RequestBody @Valid NotificationSetupRequest request) {
        try {
            return ResponseEntity.ok()
                    .body(new NotificationSetupResponse
                            (notificationService.addDevToNotifCheck(request.getDevId(), request.getProjectId()))
                    );
        } catch (IllegalAccessException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/iapi/notification/test/{projectId}")
    public ResponseEntity<Void> sendTestNotif(@PathVariable @Valid String projectId) {
        Report testReport = new Report();
        testReport.setTitle("TEST TITLE");
        testReport.setReportedAt(Instant.now());
        testReport.setId(1L);
        notificationService.sendNotifs(projectId, testReport);
        return ResponseEntity.ok().build();
    }
}

