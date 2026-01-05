package uni.bugtracker.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json; charset=UTF-8");

        String json = """
        {
          "errorCode": "ACCESS_DENIED",
          "message": "%s",
          "status": 403,
          "path": "%s",
          "timestamp": "%s"
        }
        """.formatted(
                ex.getMessage(),
                request.getRequestURI(),
                java.time.LocalDateTime.now()
        );

        response.getWriter().write(json);
    }
}

