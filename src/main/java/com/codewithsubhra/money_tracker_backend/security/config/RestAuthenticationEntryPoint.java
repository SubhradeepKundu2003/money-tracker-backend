package com.codewithsubhra.money_tracker_backend.security.config;

import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse;
import com.codewithsubhra.money_tracker_backend.common.web.ApiResponse.ApiError;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Returns the uniform JSON error envelope for unauthenticated requests (401). */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.fail(
                new ApiError("UNAUTHORIZED", "Authentication required", null));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
