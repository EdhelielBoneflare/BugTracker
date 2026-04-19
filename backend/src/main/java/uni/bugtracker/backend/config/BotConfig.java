package uni.bugtracker.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.service.CriticalBugBot;

@Getter
@Configuration
public class BotConfig {
    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(
            @Value("${telegram.bot.token}") String botToken,
            CriticalBugBot criticalBugBot
    ) throws Exception {
        TelegramBotsLongPollingApplication app = new TelegramBotsLongPollingApplication();
        app.registerBot(botToken, criticalBugBot);
        return app;
    }

}
