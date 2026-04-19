package uni.bugtracker.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
    private final TelegramClient telegramClient;

    private final DeveloperNotificationRepository devRepo;


    public CriticalBugBot(
            @Value("${telegram.bot.token}") String botToken,
            DeveloperNotificationRepository devRepo
    ) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.devRepo = devRepo;
    }

    @Override
    public void consume(Update update) {
        // This method is called every time bot receives a message
        log.info("Received update: {}", update);
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            log.info("Message text: {}, chatId: {}", text, chatId);

            if (text.startsWith("/start")) {
                String token = text.replace("/start", "").trim();

                handleStartCommand(chatId, token);
            }
        }
    }

    private void handleStartCommand(long chatId, String token) {
        Optional<DeveloperNotification> userOpt = devRepo.findByRegTokenAndStates(token, List.of(DeveloperNotification.State.REGISTERING, DeveloperNotification.State.ACTIVE));

        if (userOpt.isPresent()) {
            DeveloperNotification user = userOpt.get();
            if (user.getState().equals(DeveloperNotification.State.REGISTERING)) {
                user.setState(DeveloperNotification.State.ACTIVE);
                user.setChatId(chatId);
                devRepo.save(user);

                sendMessage(chatId, "You're now subscribed to critical bug alerts!");
            } else {
                sendMessage(chatId, "You're already subscribed to critical bug alerts!");
            }
        } else {
            sendMessage(chatId, "Invalid or expired link. Please contact your administrator.");
        }
    }

    public void sendCriticalAlert(String projectId, String bugMessage) {
        List<DeveloperNotification> activeNotifs = devRepo.findByProjectIdAndStates(projectId, List.of(DeveloperNotification.State.ACTIVE));

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
