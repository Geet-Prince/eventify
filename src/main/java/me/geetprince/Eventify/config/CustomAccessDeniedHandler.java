package me.geetprince.Eventify.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String msg = accessDeniedException != null && accessDeniedException.getMessage() != null
                ? accessDeniedException.getMessage().replace("\"", "\\\"")
                : "You do not have permission to access this resource";

        String json = String.format(
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                msg,
                LocalDateTime.now()
        );

        response.getWriter().write(json);
    }
}
