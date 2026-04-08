package uni.bugtracker.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.model.DeveloperNotification;
import uni.bugtracker.backend.model.Report;
import uni.bugtracker.backend.repository.DeveloperNotificationRepository;

import java.util.UUID;

@Service
public class NotificationService {

    @Value("${telegram.bot.username}")
    private String botUsername;

    private DeveloperNotificationRepository devNotifRepo;
    private CriticalBugBot bot;

    public String addDevToNotif(String devId, String projectId) {
        String token = UUID.randomUUID().toString();

        DeveloperNotification notif = new DeveloperNotification();
        notif.setDevId(devId);
        notif.setProjectId(projectId);
        notif.setState(DeveloperNotification.state.REGISTERING.name());

        devNotifRepo.save(notif);

        if (botUsername == null || botUsername.isBlank()) {
            throw new IllegalStateException("Telegram bot username is not configured");
        }

        return "https://t.me/" + botUsername + "?start=" + token;
    }

    public void sendNotifs(String projectId, Report report) {
        String message = "id: %s, title: %s, reported at: %s".formatted(report.getId(), report.getTitle(), report.getReportedAt());
        bot.sendCriticalAlert(projectId, message);
    }
}
