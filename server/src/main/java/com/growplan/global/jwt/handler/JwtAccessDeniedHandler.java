package com.growplan.global.jwt.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growplan.global.error.ApiResponse;
import com.growplan.global.error.code.status.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws
            IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(403);

        ApiResponse<Object> errorResponse = ApiResponse.onFailure(
                ErrorStatus.FORBIDDEN.getCode(),
                ErrorStatus.FORBIDDEN.getMessage(),
                null
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
