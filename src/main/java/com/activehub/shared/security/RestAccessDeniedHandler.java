package com.activehub.shared.security;

import com.activehub.shared.error.ApiError;
import com.activehub.shared.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.SIN_PERMISO.getStatus().value(),
                ApiErrorCode.SIN_PERMISO.name(),
                "No tenés permiso para realizar esta acción.",
                null,
                request.getRequestURI()
        );
        response.setStatus(ApiErrorCode.SIN_PERMISO.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
