package com.growplan.domain.goal.dto;

import com.growplan.global.common.enums.InvestmentPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

public class GoalResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GoalAnalysisResponseDTO {

        BigDecimal totalAmount; // 총 자산
        String analysisText; // 목표 분석
        BigDecimal targetAmount; // 목표 금액
        BigDecimal emergencyFundAmount; // 비상금 마련 금액
        InvestmentPurpose investmentPurpose; // 투자 목적
    }
}
