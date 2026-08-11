package com.activehub.shared.security;

import com.activehub.shared.error.ApiError;
import com.activehub.shared.error.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ApiError body = new ApiError(
                Instant.now(),
                ApiErrorCode.CREDENCIALES_INVALIDAS.getStatus().value(),
                ApiErrorCode.CREDENCIALES_INVALIDAS.name(),
                "Necesitás iniciar sesión para acceder a este recurso.",
                null,
                request.getRequestURI()
        );
        response.setStatus(ApiErrorCode.CREDENCIALES_INVALIDAS.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
