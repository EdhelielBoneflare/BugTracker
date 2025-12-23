package uni.bugtracker.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uni.bugtracker.backend.dto.session.SessionCreationResponse;
import uni.bugtracker.backend.dto.session.SessionDetailsResponse;
import uni.bugtracker.backend.dto.session.SessionRequest;
import uni.bugtracker.backend.service.SessionService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@AutoConfigureJsonTesters
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<SessionRequest> sessionRequestJson;

    @MockitoBean
    private SessionService sessionService;

    private SessionRequest sessionRequest;
    private SessionCreationResponse sessionCreationResponse;
    private SessionDetailsResponse mockSessionDetailsResponse;

    @BeforeEach
    void setUp() {
        sessionRequest = new SessionRequest();
        sessionRequest.setProjectId(1L);
        sessionRequest.setStartTime(Instant.now());
        sessionRequest.setBrowser("Chrome");
        sessionRequest.setBrowserVersion("120.0.0.0");
        sessionRequest.setOs("Windows 10");
        sessionRequest.setDeviceType("desktop");
        sessionRequest.setScreenResolution("1920x1080");
        sessionRequest.setViewportSize("1920x947");
        sessionRequest.setLanguage("en-US");
        sessionRequest.setUserAgent("Mozilla/5.0...");
        sessionRequest.setIpAddress("192.168.1.1");
        sessionRequest.setCookiesHash("abc123hash");
        sessionRequest.setPlugins(List.of("Adobe Flash", "Java"));

        sessionCreationResponse = new SessionCreationResponse(
                "Session created successfully",
                1L
        );

        mockSessionDetailsResponse = mock(SessionDetailsResponse.class);
        when(mockSessionDetailsResponse.getSessionId()).thenReturn(1L);
        when(mockSessionDetailsResponse.getProjectId()).thenReturn(1L);
        when(mockSessionDetailsResponse.getIsActive()).thenReturn(true);
        when(mockSessionDetailsResponse.getStartTime()).thenReturn(Instant.now());
        when(mockSessionDetailsResponse.getBrowser()).thenReturn("Chrome");
        when(mockSessionDetailsResponse.getBrowserVersion()).thenReturn("120.0.0.0");
        when(mockSessionDetailsResponse.getOs()).thenReturn("Windows 10");
        when(mockSessionDetailsResponse.getDeviceType()).thenReturn("desktop");
        when(mockSessionDetailsResponse.getScreenResolution()).thenReturn("1920x1080");
        when(mockSessionDetailsResponse.getViewportSize()).thenReturn("1920x947");
        when(mockSessionDetailsResponse.getLanguage()).thenReturn("en-US");
        when(mockSessionDetailsResponse.getUserAgent()).thenReturn("Mozilla/5.0...");
        when(mockSessionDetailsResponse.getIpAddress()).thenReturn("192.168.1.1");
        when(mockSessionDetailsResponse.getPlugins()).thenReturn(List.of("Adobe Flash", "Java"));
    }

    @Test
    void createSession_ShouldReturnCreated() throws Exception {
        // Arrange
        when(sessionService.createSession(any(SessionRequest.class)))
                .thenReturn(sessionCreationResponse);

        // Act & Assert
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson.write(sessionRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Session created successfully"))
                .andExpect(jsonPath("$.sessionId").value(1L));
    }

    @Test
    void createSession_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        sessionRequest.setProjectId(null);
        sessionRequest.setStartTime(Instant.now().plusSeconds(3600));

        // Act & Assert
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson.write(sessionRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSession_WithInvalidDeviceType_ShouldReturnBadRequest() throws Exception {
        // Arrange
        sessionRequest.setDeviceType("invalid_device_type");

        // Act & Assert
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson.write(sessionRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSession_WithInvalidScreenResolution_ShouldReturnBadRequest() throws Exception {
        // Arrange
        sessionRequest.setScreenResolution("invalid_format");

        // Act & Assert
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson.write(sessionRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSession_ShouldReturnOk() throws Exception {
        // Arrange
        Long sessionId = 1L;
        when(sessionService.getSession(sessionId))
                .thenReturn(mockSessionDetailsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/sessions/{id}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.projectId").value(1L))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.browser").value("Chrome"))
                .andExpect(jsonPath("$.deviceType").value("desktop"));
    }

    @Test
    void getSession_WithNonExistentId_ShouldThrowException() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        when(sessionService.getSession(nonExistentId))
                .thenThrow(new uni.bugtracker.backend.exception.ResourceNotFoundException("Session not found"));

        // Act & Assert
        mockMvc.perform(get("/api/sessions/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSession_WithMissingRequiredFields_ShouldReturnBadRequest() throws Exception {
        // Arrange
        sessionRequest.setProjectId(null);
        sessionRequest.setStartTime(null);

        // Act & Assert
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson.write(sessionRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSession_WithInvalidIpAddress_ShouldReturnBadRequest() throws Exception {
        // Arrange
        sessionRequest.setIpAddress("invalid_ip_format");

        // Act & Assert
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequestJson.write(sessionRequest).getJson()))
                .andExpect(status().isBadRequest());
    }
}