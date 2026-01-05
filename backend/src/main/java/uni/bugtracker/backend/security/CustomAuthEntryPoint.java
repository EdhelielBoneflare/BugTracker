package uni.bugtracker.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// process unauthorized requests
@Component
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");

        String json = """
        {
          "errorCode": "AUTH_FAILED",
          "message": "%s",
          "status": 401,
          "path": "%s",
          "timestamp": "%s"
        }
        """.formatted(
                authException.getMessage(),
                request.getRequestURI(),
                java.time.LocalDateTime.now()
        );

        response.getWriter().write(json);

//        ErrorResponse error = new ErrorResponse("AUTH_FAILED", ex.getMessage(), 401);
//        response.getWriter().write(new ObjectMapper().writeValueAsString(error)); //LocalDate -> string
    }
}


