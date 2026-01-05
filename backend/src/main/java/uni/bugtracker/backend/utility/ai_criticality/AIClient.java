package uni.bugtracker.backend.utility.ai_criticality;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import uni.bugtracker.backend.dto.report.OpenRouterRequest;
import uni.bugtracker.backend.dto.report.OpenRouterResponse;
import uni.bugtracker.backend.model.CriticalityLevel;
import uni.bugtracker.backend.model.Event;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AIClient {
    @Value("${openrouter.api.key}")
    private String apiKey;

    private static final String SYSTEM_PROMPT = """
        You are an automated incident severity classifier.
        
        You will receive technical event logs from a user session.
        Each event may include logs, stack traces, metadata and user actions.
        
        Your task:
        Determine the overall incident criticality.
        
        Criticality levels:
        LOW - minor issues, no user impact
        MEDIUM - partial degradation, noticeable issues
        HIGH - major malfunction, core feature broken
        CRITICAL - crashes, data loss, security issues
        
        Rules:
        - Base your decision ONLY on provided events
        - Return EXACTLY one word
        - Allowed values: LOW, MEDIUM, HIGH, CRITICAL
        - No explanations
        - No punctuation
        """;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .build();

    public CriticalityLevel analyze(List<Event> events) {

        try {
            OpenRouterRequest request = new OpenRouterRequest(
                    "openai/gpt-oss-120b:free",
                    List.of(
                            new OpenRouterRequest.Message("system", SYSTEM_PROMPT),
                            new OpenRouterRequest.Message("user", buildUserPrompt(events))
                    ),
                    new OpenRouterRequest.Reasoning(true)
            );

            OpenRouterResponse response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenRouterResponse.class)
                    .block(Duration.ofSeconds(30));

            if (response == null || response.getChoices().isEmpty()) {
                log.warn("Empty AI response");
                System.out.println("===== Empty AI response =====");
                return CriticalityLevel.MEDIUM;
            }

            String raw = response.getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent()
                    .trim()
                    .toUpperCase();

            return parseOrDefault(raw);
        } catch (Exception ex) {
            log.error("AI criticality analysis failed", ex);
            System.out.println("===== AI criticality analysis failed: " + ex + "=====");
            return CriticalityLevel.MEDIUM;
        }
    }

    private CriticalityLevel parseOrDefault(String raw) {
        try {
            System.out.println("We are in parseOrDefault, raw = " + raw);
            return CriticalityLevel.valueOf(raw);
        } catch (Exception ex) {
            log.warn("Unrecognized AI criticality response: {}", raw);
            System.out.println("===== Unrecognized AI criticality response: " + raw + "=====");
            return CriticalityLevel.MEDIUM;
        }
    }

    private String buildUserPrompt(List<Event> events) {
        StringBuilder sb = new StringBuilder("Session events:\n");

        for (Event e : events) {
            sb.append("""
        ---
        timestamp: %s
        type: %s
        name: %s
        url: %s
        element: %s
        log: %s
        stackTrace: %s
        """.formatted(
                    e.getTimestamp(),
                    e.getType(),
                    e.getName(),
                    e.getUrl(),
                    e.getElement(),
                    truncate(e.getLog()),
                    truncate(e.getStackTrace())
            ));
        }

        sb.append("\nReturn the incident criticality level.");

        return sb.toString();
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 20_000 ? value.substring(0, 20_000) : value;
    }
}
