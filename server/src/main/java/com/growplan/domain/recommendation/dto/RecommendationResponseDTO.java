package com.growplan.domain.recommendation.dto;

import com.growplan.global.common.enums.*;
import lombok.*;

import java.math.BigDecimal;

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

        private BigDecimal totalAmount;
        private IncomeRange incomeRange;
        private InvestmentPeriod investmentPeriod;
        private Propensity propensity;
    }
}
