package uni.bugtracker.backend.dto.report;

import lombok.Getter;

import java.util.List;

@Getter
public class OpenRouterResponse {
    private List<Choice> choices;

    @Getter
    public static class Choice {
        private Message message;
    }

    @Getter
    public static class Message {
        private String content;
    }
}
