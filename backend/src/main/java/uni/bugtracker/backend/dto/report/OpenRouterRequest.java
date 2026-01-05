package uni.bugtracker.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OpenRouterRequest {
    private String model;
    private List<Message> messages;
    private Reasoning reasoning;

    @Getter
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Getter
    @AllArgsConstructor
    public static class Reasoning {
        private boolean enabled;
    }
}
