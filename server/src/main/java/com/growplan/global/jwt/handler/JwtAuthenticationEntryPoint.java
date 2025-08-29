package com.growplan.global.jwt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growplan.global.error.ApiResponse;
import com.growplan.global.error.code.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws
            IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(401);

        ApiResponse<Object> errorResponse = ApiResponse.onFailure(
                ErrorStatus.UNAUTHORIZED.getCode(),
                ErrorStatus.UNAUTHORIZED.getMessage(),
                null
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
