package com.growplan.domain.recommendation.dto;

import com.growplan.global.common.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

public class RecommendationResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIPortfolioResponse {

        private BigDecimal cashAmount;
        private BigDecimal cashRatioPct;

        private BigDecimal depositAmount;
        private BigDecimal depositRatioPct;

        private BigDecimal savingsAmount;
        private BigDecimal savingsRatioPct;

        private BigDecimal investmentAmount;
        private BigDecimal investmentRatioPct;

        private String recommendationMessage;

        private BigDecimal totalAmount;
        private IncomeRange incomeRange;
        private InvestmentPeriod investmentPeriod;
        private Propensity propensity;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EFResult {
        private Map<String, BigDecimal> weightsPct;
        private BigDecimal annualReturnPct;
        private BigDecimal annualVolPct;
        private BigDecimal sharpe;
        private BigDecimal maxDrawdownPct;
    }

    // ▼ 신규: /etf 응답
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EtfSnapshot {
        private Map<String, BigDecimal> lastPrices;
        private Map<String, BigDecimal> dayChangePct;
        private String asOf;
        private EFResult efResult; // 가중치/지표
    }
}
