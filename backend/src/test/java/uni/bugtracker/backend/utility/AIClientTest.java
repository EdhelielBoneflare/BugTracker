package uni.bugtracker.backend.utility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uni.bugtracker.backend.dto.report.OpenRouterRequest;
import uni.bugtracker.backend.dto.report.OpenRouterResponse;
import uni.bugtracker.backend.model.CriticalityLevel;
import uni.bugtracker.backend.model.Event;
import uni.bugtracker.backend.model.EventType;
import uni.bugtracker.backend.utility.ai_criticality.AIClient;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIClientTest {

    private AIClient aiClient;
    private WebClient.RequestBodyUriSpec requestBodyUriSpecMock;
    private WebClient.RequestBodySpec requestBodySpecMock;
    private WebClient.ResponseSpec responseSpecMock;

    @BeforeEach
    void setUp() throws Exception {
        WebClient webClientMock = mock(WebClient.class);
        requestBodyUriSpecMock = mock(WebClient.RequestBodyUriSpec.class);
        requestBodySpecMock = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> requestHeadersSpecMock = mock(WebClient.RequestHeadersSpec.class);
        responseSpecMock = mock(WebClient.ResponseSpec.class);

        when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
        when(requestBodyUriSpecMock.uri(anyString())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.header(anyString(), anyString())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.headers(any())).thenReturn(requestBodySpecMock);
        when(requestBodySpecMock.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);

        aiClient = new AIClient();

        Field apiKeyField = AIClient.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(aiClient, "test-api-key");

        Field webClientField = AIClient.class.getDeclaredField("webClient");
        webClientField.setAccessible(true);
        webClientField.set(aiClient, webClientMock);
    }

    @Test
    void analyze_shouldReturnCriticalityLevelForValidResponse() {
        // Given
        List<Event> events = createSampleEvents();

        OpenRouterResponse.Message message = new OpenRouterResponse.Message();
        setPrivateField(message, "content", "HIGH");

        OpenRouterResponse.Choice choice = new OpenRouterResponse.Choice();
        setPrivateField(choice, "message", message);

        OpenRouterResponse mockResponse = new OpenRouterResponse();
        setPrivateField(mockResponse, "choices", List.of(choice));

        when(responseSpecMock.bodyToMono(OpenRouterResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // When
        CriticalityLevel result = aiClient.analyze(events);

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.HIGH);

        // Verify WebClient calls
        verify(requestBodyUriSpecMock).uri("/chat/completions");
        verify(requestBodySpecMock).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        verify(requestBodySpecMock).header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key");
        verify(requestBodySpecMock).bodyValue(any(OpenRouterRequest.class));
    }

    @Test
    void analyze_shouldReturnMediumWhenResponseIsNull() {
        // Given
        List<Event> events = createSampleEvents();

        when(responseSpecMock.bodyToMono(OpenRouterResponse.class))
                .thenReturn(Mono.empty());

        // When
        CriticalityLevel result = aiClient.analyze(events);

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void analyze_shouldReturnMediumWhenChoicesEmpty() {
        // Given
        List<Event> events = createSampleEvents();

        OpenRouterResponse mockResponse = new OpenRouterResponse();
        setPrivateField(mockResponse, "choices", List.of());

        when(responseSpecMock.bodyToMono(OpenRouterResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // When
        CriticalityLevel result = aiClient.analyze(events);

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void analyze_shouldHandleNetworkTimeout() {
        // Given
        List<Event> events = createSampleEvents();

        when(responseSpecMock.bodyToMono(OpenRouterResponse.class))
                .thenReturn(Mono.error(new RuntimeException("Timeout")));

        // When
        CriticalityLevel result = aiClient.analyze(events);

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void analyze_shouldHandleWebClientException() {
        // Given
        List<Event> events = createSampleEvents();

        when(responseSpecMock.bodyToMono(OpenRouterResponse.class))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        500, "Server Error", HttpHeaders.EMPTY, null, null)));

        // When
        CriticalityLevel result = aiClient.analyze(events);

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void parseOrDefault_shouldParseValidCriticalityLevels() throws Exception {
        // Given
        String[] validLevels = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};

        // When & Then
        for (String level : validLevels) {
            CriticalityLevel result = invokePrivateMethod("parseOrDefault", level);
            assertThat(result).isEqualTo(CriticalityLevel.valueOf(level));
        }
    }

    @Test
    void parseOrDefault_shouldReturnMediumForInvalidString() throws Exception {
        // Given
        String invalidLevel = "INVALID_LEVEL";

        // When
        CriticalityLevel result = invokePrivateMethod("parseOrDefault", invalidLevel);

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void parseOrDefault_shouldReturnMediumForEmptyString() throws Exception {
        // When
        CriticalityLevel result = invokePrivateMethod("parseOrDefault", "");

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void parseOrDefault_shouldReturnMediumForNull() throws Exception {
        // When
        CriticalityLevel result = invokePrivateMethod("parseOrDefault", (String) null);  // Cast to String

        // Then
        assertThat(result).isEqualTo(CriticalityLevel.MEDIUM);
    }

    @Test
    void buildUserPrompt_shouldFormatEventsCorrectly() throws Exception {
        // Given
        List<Event> events = createSampleEvents();

        // When
        String prompt = invokePrivateMethod("buildUserPrompt", events);

        // Then
        assertThat(prompt).contains("Session events:");
        assertThat(prompt).contains("timestamp: 2024-01-01T10:00:00Z");
        assertThat(prompt).contains("type: ERROR");
        assertThat(prompt).contains("name: Test Error");
        assertThat(prompt).contains("url: http://example.com");
        assertThat(prompt).contains("log: Test log message");
        assertThat(prompt).contains("stackTrace: at com.example.Test");
        assertThat(prompt).contains("Return the incident criticality level.");
    }

    @Test
    void buildUserPrompt_shouldHandleNullEventFields() throws Exception {
        // Given
        Event event = new Event();
        event.setTimestamp(Instant.now());
        event.setType(EventType.ERROR);
        event.setName("Error");
        List<Event> events = List.of(event);

        // When
        String prompt = invokePrivateMethod("buildUserPrompt", events);

        // Then
        assertThat(prompt).contains("log: null");
        assertThat(prompt).contains("stackTrace: null");
    }

    @Test
    void truncate_shouldReturnSameStringWhenShort() throws Exception {
        // Given
        String shortString = "Short string";

        // When
        String result = invokePrivateMethod("truncate", shortString);

        // Then
        assertThat(result).isEqualTo(shortString);
    }

    @Test
    void truncate_shouldTruncateLongStrings() throws Exception {
        // Given
        String longString = "x".repeat(25000); // 25k символов

        // When
        String result = invokePrivateMethod("truncate", longString);

        // Then
        assertThat(result).hasSize(20000);
        assertThat(result).isEqualTo("x".repeat(20000));
    }

    @Test
    void truncate_shouldHandleNull() throws Exception {
        // When
        String result = invokePrivateMethod("truncate", (Object) null);
        // Then
        assertThat(result).isNull();
    }


    @Test
    void truncate_shouldHandleEmptyString() throws Exception {
        // When
        String result = invokePrivateMethod("truncate", "");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void analyze_shouldIncludeElementInPrompt() {
        // Given
        Event event = new Event();
        event.setTimestamp(Instant.now());
        event.setType(EventType.ACTION);
        event.setName("Button Click");
        event.setUrl("http://example.com");
        event.setElement("#submit-button");
        event.setLog("User clicked submit button");

        List<Event> events = List.of(event);

        OpenRouterResponse.Message message = new OpenRouterResponse.Message();
        setPrivateField(message, "content", "MEDIUM");

        OpenRouterResponse.Choice choice = new OpenRouterResponse.Choice();
        setPrivateField(choice, "message", message);

        OpenRouterResponse mockResponse = new OpenRouterResponse();
        setPrivateField(mockResponse, "choices", List.of(choice));

        when(responseSpecMock.bodyToMono(OpenRouterResponse.class))
                .thenReturn(Mono.just(mockResponse));

        // When
        aiClient.analyze(events);

        // Then
        verify(requestBodySpecMock).bodyValue(argThat((OpenRouterRequest req) -> {
            List<OpenRouterRequest.Message> messages = req.getMessages();
            if (messages.size() > 1) {
                String userMessage = messages.get(1).getContent();
                return userMessage.contains("element: #submit-button");
            }
            return false;
        }));
    }

    private List<Event> createSampleEvents() {
        Event event1 = new Event();
        event1.setTimestamp(Instant.parse("2024-01-01T10:00:00Z"));
        event1.setType(EventType.ERROR);
        event1.setName("Test Error");
        event1.setLog("Test log message");
        event1.setStackTrace("at com.example.Test");
        event1.setUrl("http://example.com");
        event1.setElement("#test-button");

        return List.of(event1);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(String methodName, Object... args) throws Exception {
        if (args == null) {
            args = new Object[]{null};
        }

        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                paramTypes[i] = Object.class;
            } else {
                paramTypes[i] = args[i].getClass();
            }
        }

        var method = Arrays.stream(AIClient.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName) &&
                        m.getParameterTypes().length == paramTypes.length)
                .findFirst()
                .orElseThrow(() -> new NoSuchMethodException("Method " + methodName + " not found"));

        method.setAccessible(true);

        return (T) method.invoke(aiClient, args);
    }


    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}