package com.growplan.domain.asset.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class AssetResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterAssetResponseDTO {
        BigDecimal totalAmount;

        BigDecimal cashAmount;
        BigDecimal cashRatioPercent;        // 총자산 대비 %
        BigDecimal investedAmount;
        BigDecimal investedRatioPercent;    // 총자산 대비 %

        BigDecimal otherAmount;
        BigDecimal otherRatioPercent;       // 총자산 대비 %

        // 투자자산 내 비중 (%)
        BigDecimal stockRatioPercent;
        BigDecimal bitcoinRatioPercent;
        BigDecimal bondRatioPercent;
        BigDecimal etfRatioPercent;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class updateInvestmentResponseDTO {

        BigDecimal stockAmount;
        BigDecimal bitcoinAmount;
        BigDecimal bondAmount;
        BigDecimal etfAmount;
    }
}
