package com.growplan.global.token.converter;

import com.growplan.global.token.dto.TokenResponseDTO;

public class TokenConverter {
    public static TokenResponseDTO.TokenDTO toTokenDTO(String accessToken) {
        return TokenResponseDTO.TokenDTO.builder()
                .accessToken(accessToken)
                .build();
    }
}