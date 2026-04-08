package uni.bugtracker.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import uni.bugtracker.backend.model.DeveloperNotification;
import uni.bugtracker.backend.repository.DeveloperNotificationRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class CriticalBugBot implements LongPollingSingleThreadUpdateConsumer {
    @Value("${telegram.bot.token}")
    private String botToken;

    private TelegramClient telegramClient = new OkHttpTelegramClient(botToken);

    private DeveloperNotificationRepository devRepo;

    @Override
    public void consume(Update update) {
        // This method is called every time bot receives a message
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.startsWith("/start")) {
                String token = text.replace("/start", "").trim();

                handleStartCommand(chatId, token);
            }
        }
    }

    private void handleStartCommand(long chatId, String token) {
        Optional<DeveloperNotification> userOpt = devRepo.findByRegTokenAndStates(token, List.of(DeveloperNotification.state.REGISTERING, DeveloperNotification.state.ACTIVE));

        if (userOpt.isPresent()) {
            DeveloperNotification user = userOpt.get();
            user.setState("active");
            user.setChatId(chatId);
            devRepo.save(user);

            sendMessage(chatId, "You're now subscribed to critical bug alerts!");
        } else {
            sendMessage(chatId, "Invalid or expired link. Please contact your administrator.");
        }
    }

    public void sendCriticalAlert(String projectId, String bugMessage) {
        List<DeveloperNotification> activeNotifs = devRepo.findByProjectIdAndStates(projectId, List.of(DeveloperNotification.state.ACTIVE));

        for (DeveloperNotification notif : activeNotifs) {
            if (notif.getChatId() != null) {
                sendMessage(notif.getChatId(), "CRITICAL BUG: \n" + bugMessage);
            }
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}", chatId, e);
        }
    }
}
