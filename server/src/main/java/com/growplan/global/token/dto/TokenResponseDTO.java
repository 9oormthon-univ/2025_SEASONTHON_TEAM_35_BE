package com.growplan.global.token.dto;


import lombok.*;

public class TokenResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class TokenDTO {
        String accessToken;
    }
}
