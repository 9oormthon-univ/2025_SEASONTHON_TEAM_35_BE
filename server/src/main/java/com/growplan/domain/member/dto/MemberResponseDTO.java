package com.growplan.domain.member.dto;

import com.growplan.domain.asset.entity.AssetPortfolio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class MemberResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginSuccessDTO {

        String accessToken;
        String name;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyPageDTO {
        String name;
        String email;
        BigDecimal totalAmount;
    }
}
