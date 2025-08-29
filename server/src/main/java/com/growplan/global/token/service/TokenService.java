package com.growplan.global.token.service;

import com.growplan.global.token.dto.TokenResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface TokenService {
    TokenResponseDTO.TokenDTO reissueAccessToken(HttpServletRequest request, HttpServletResponse response);
}
