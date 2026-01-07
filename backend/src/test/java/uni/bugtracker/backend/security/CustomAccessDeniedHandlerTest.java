package uni.bugtracker.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAccessDeniedHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CustomAccessDeniedHandler handler;

    @Test
    void handle_shouldReturnCorrectForbiddenResponse() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("/api/projects/123");
        when(response.getWriter()).thenReturn(printWriter);

        AccessDeniedException exception = new AccessDeniedException("Access denied to project");

        // When
        handler.handle(request, response, exception);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json; charset=UTF-8");

        String json = writer.toString().trim();

        assertThat(json).contains("\"errorCode\": \"ACCESS_DENIED\"");
        assertThat(json).contains("\"message\": \"Access denied to project\"");
        assertThat(json).contains("\"status\": 403");
        assertThat(json).contains("\"path\": \"/api/projects/123\"");
        assertThat(json).contains("\"timestamp\":");
    }

    @Test
    void handle_shouldHandleEmptyRequestPath() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("");
        when(response.getWriter()).thenReturn(printWriter);

        AccessDeniedException exception = new AccessDeniedException("Test");

        // When
        handler.handle(request, response, exception);

        // Then
        String json = writer.toString().trim();
        assertThat(json).contains("\"path\": \"\"");
    }

    @Test
    void handle_shouldHandleNullMessage() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(printWriter);

        AccessDeniedException exception = new AccessDeniedException(null);

        // When
        handler.handle(request, response, exception);

        // Then
        String json = writer.toString().trim();
        assertThat(json).contains("\"message\": \"null\"");
    }
}