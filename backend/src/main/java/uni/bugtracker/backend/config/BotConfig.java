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
    public void telegramBotsApi(CriticalBugBot criticalBugBot) throws TelegramApiException {
        try {
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, criticalBugBot);
        } catch (TelegramApiException e) {
            throw new ResourceNotFoundException("TG ERROR: " + e.getMessage());
        }
    }
}
