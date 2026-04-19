package uni.bugtracker.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.dto.notification.NotificationStateResponse;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.DeveloperNotification;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.model.Report;
import uni.bugtracker.backend.repository.DeveloperNotificationRepository;
import uni.bugtracker.backend.repository.DeveloperRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    @Value("${telegram.bot.username}")
    private String botUsername;

    private final DeveloperNotificationRepository devNotifRepo;
    private final CriticalBugBot bot;
    private final DeveloperRepository devRepo;

    public String addDevToNotifCheck(String devId, String projectId) throws IllegalAccessException {
        Developer dev = devRepo.findById(devId)
                .orElseThrow(() -> new ResourceNotFoundException("Couldn't find user with provided id."));

        boolean hasProject = false;
        for (Project project: dev.getProjects()) {
            if (project.getId().equals(projectId)) {
                hasProject = true;
                break;
            }
        }

        if (!hasProject) {
            throw new IllegalAccessException("This developer doesn't have access to this project.");
        }

        return addDevToNotif(devId, projectId);
    }



    private String addDevToNotif(String devId, String projectId) {
        String token = UUID.randomUUID().toString();

        DeveloperNotification notif = null;
        Optional<DeveloperNotification> existingNotification = devNotifRepo
                .findByDevIdAndProjectId(devId, projectId);

        if (existingNotification.isPresent()) {
            DeveloperNotification existing = existingNotification.get();
            if (existing.getState() == DeveloperNotification.State.ACTIVE ||
                    existing.getState() == DeveloperNotification.State.REGISTERING) {
                notif = existing;
            }
        }

        if (notif == null) {
            notif = new DeveloperNotification();
            notif.setDevId(devId);
            notif.setProjectId(projectId);
            notif.setState(DeveloperNotification.State.REGISTERING);
            notif.setRegToken(token);
            devNotifRepo.save(notif);
        }

        return buildBotRegistrationUrl(notif.getRegToken());
    }

    public List<NotificationStateResponse> getNotificationStates(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return List.of();
        }

        List<DeveloperNotification> notifications = devNotifRepo.findByProjectId(projectId);

        return notifications.stream()
                .map(NotificationStateResponse::fromDevNotif)
                .toList();
    }

    private void validateBotConfiguration() {
        if (botUsername == null || botUsername.isBlank()) {
            throw new IllegalStateException("Telegram bot username is not configured");
        }
    }

    private String buildBotRegistrationUrl(String token) {
        validateBotConfiguration();
        return "https://t.me/" + botUsername + "?start=" + token;
    }

    public void sendNotifs(String projectId, Report report) {
        String message = "id: %s, title: %s, reported at: %s".formatted(report.getId(), report.getTitle(), report.getReportedAt());
        bot.sendCriticalAlert(projectId, message);
    }
}
