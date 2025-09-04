package com.growplan.domain.recommendation.dto;

import com.growplan.global.common.enums.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class RecommendationRequestDTO {

    @Getter
    @Setter
    public static class InvestmentDesignRequest {

        private SavingRange savingRange; // 월 저축 가능 금액
        private IncomeRange incomeRange; // 월 소득 범위
        private ProfitRange profitRange; // 기대 수익률
        private InvestmentPeriod investmentPeriod; // 투자 기간
        private Propensity propensity; // 투자 성향
        private InvestmentPurpose investmentPurpose; // 투자 목적
        private Boolean emergencyFund;
    }

    @Getter
    @Setter
    public static class ExternalRecommendRequest {
        private List<String> assets;
        private int risk_level;
        private int lookback_years;
        private double rf;
        private int points;
    }
}
