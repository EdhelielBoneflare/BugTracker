package uni.bugtracker.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomAuthEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private CustomAuthEntryPoint authEntryPoint;

    @Test
    void commence_shouldReturnCorrectUnauthorizedResponse() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("/api/admin/users");
        when(response.getWriter()).thenReturn(printWriter);

        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        // When
        authEntryPoint.commence(request, response, exception);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json; charset=UTF-8");

        String json = writer.toString().trim();

        assertThat(json).contains("\"errorCode\": \"AUTH_FAILED\"");
        assertThat(json).contains("\"message\": \"Invalid credentials\"");
        assertThat(json).contains("\"status\": 401");
        assertThat(json).contains("\"path\": \"/api/admin/users\"");
        assertThat(json).contains("\"timestamp\":");
    }

    @Test
    void commence_shouldHandleDifferentExceptionTypes() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("/api/login");
        when(response.getWriter()).thenReturn(printWriter);

        AuthenticationException exception =
                new org.springframework.security.authentication.DisabledException("Account is disabled");

        // When
        authEntryPoint.commence(request, response, exception);

        // Then
        String json = writer.toString().trim();
        assertThat(json).contains("\"message\": \"Account is disabled\"");
        assertThat(json).contains("\"errorCode\": \"AUTH_FAILED\"");
    }

    @Test
    void commence_shouldHandleNullMessage() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(printWriter);

        AuthenticationException exception = new AuthenticationException(null) {};

        // When
        authEntryPoint.commence(request, response, exception);

        // Then
        String json = writer.toString().trim();
        assertThat(json).contains("\"message\": \"null\"");
    }

    @Test
    void commence_shouldSetCorrectHttpHeaders() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(printWriter);

        AuthenticationException exception = new BadCredentialsException("Test");

        // When
        authEntryPoint.commence(request, response, exception);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json; charset=UTF-8");
    }

    @Test
    void commence_shouldWriteValidJsonFormat() throws IOException {
        // Given
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);

        when(request.getRequestURI()).thenReturn("/api/health");
        when(response.getWriter()).thenReturn(printWriter);

        AuthenticationException exception = new BadCredentialsException("Simple test");

        // When
        authEntryPoint.commence(request, response, exception);

        // Then
        String json = writer.toString().trim();
        assertThat(json).startsWith("{");
        assertThat(json).endsWith("}");
        assertThat(json).contains("errorCode");
        assertThat(json).contains("message");
        assertThat(json).contains("status");
        assertThat(json).contains("path");
        assertThat(json).contains("timestamp");
    }
}