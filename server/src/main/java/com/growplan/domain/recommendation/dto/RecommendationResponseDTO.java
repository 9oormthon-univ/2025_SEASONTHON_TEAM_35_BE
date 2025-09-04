package com.growplan.domain.recommendation.dto;

import com.growplan.global.common.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
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
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendApiResult {
        private double annual_return;
        private double annual_vol;
        private double sharpe;
        private double max_drawdown;
        private Map<String, Double> weights;
        private long elapsed_ms;

        private String reasonText;   // 투자 비율 추천 이유

        List<AssetCard> etfList;

        private String horizon_title;             // "6개월" / "1년" / "2년" / "3년" / "5년" / "10년"
        private Integer point_count;              // 3 / 4 / 6
        private BigDecimal base_amount;           // 현재 총자산
        private InvestmentPeriod period;          // 사용자가 입력한 투자기간
        private List<ForecastPoint> forecast_points; // 차트 포인트 배열
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastPoint {
        private String label;          // "현재", "3개월 후", "1년 후" ...
        private double years;          // 0, 0.25, 1, 2, ...
        private BigDecimal amount;     // 미래값(FV)
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetCard {
        private String symbol;          // 예: "QQQM", "277630.KS"
        private String etf_name;        // 예: "Invesco NASDAQ 100 ETF", "TIGER 코스피"
        private BigDecimal price;       // 예: 234.81, 33090, 109495
        private String currency;        // 예: "USD", "KRW"
        private Double day_change_pct;  // 예: -1.17, -0.17, -0.03 (단위 %)
    }
}
