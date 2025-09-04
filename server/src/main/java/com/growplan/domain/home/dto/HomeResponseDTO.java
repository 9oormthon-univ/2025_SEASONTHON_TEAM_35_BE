package com.growplan.domain.home.dto;

import com.growplan.domain.goal.dto.GoalResponseDTO;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;
import com.growplan.global.common.enums.InvestmentPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class HomeResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewDTO {

        private String name;
        private BigDecimal totalAsset;

        // 자산 비율
        private int cashRatio;
        private int depositRatio;
        private int savingRatio;
        private int investRatio;
        private int etcRatio;

        private InvestmentForecastDTO investmentForecast;

        private BigDecimal targetAmount;
        private int achievementRate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentForecastDTO {
        private String horizonTitle;   // "3년" 등
        private Integer pointCount;    // 3/4/6
        private BigDecimal currentAmount; // 현재 총자산
        private InvestmentPeriod period;
        private List<RecommendationResponseDTO.ForecastPoint> forecastPoints;
    }
}
