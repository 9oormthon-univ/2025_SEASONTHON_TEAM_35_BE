package com.growplan.domain.asset.converter;

import com.growplan.domain.asset.dto.AssetRequestDTO;
import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.asset.entity.AssetItem;
import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.member.entity.Member;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AssetConverter {

    private static final int PCT_SCALE = 1;
    private static final RoundingMode RM = RoundingMode.HALF_UP; // 반올림

    public static AssetPortfolio toAssetPortfolio(Member member) {
        return AssetPortfolio.builder()
                .member(member)
                .totalAmount(BigDecimal.ZERO)
                .totalInvestedAmount(BigDecimal.ZERO)
                .build();
    }

    public static AssetItem toAssetItem(AssetRequestDTO.ItemRequest request) {
        return AssetItem.builder()
                .assetType(request.getAssetType())
                .amount(request.getAmount())
                .build();
    }

    public static AssetResponseDTO.RegisterAssetResponseDTO toRegisterAssetResponse (
            BigDecimal total,
            BigDecimal cash,
            BigDecimal deposit,
            BigDecimal savings,
            BigDecimal invested,
            BigDecimal other,
            BigDecimal stock,
            BigDecimal bitcoin,
            BigDecimal bond,
            BigDecimal etf
    ) {
        total = nz(total);
        cash = nz(cash);
        invested = nz(invested);
        other = nz(other);

        return AssetResponseDTO.RegisterAssetResponseDTO.builder()
                .totalAmount(total)

                .cashAmount(cash)
                .cashRatioPercent(percent(cash, total))

                .depositAmount(deposit)
                .depositRatioPercent(percent(deposit, total))

                .savingsAmount(savings)
                .savingsRatioPercent(percent(savings, total))

                .investedAmount(invested)
                .investedRatioPercent(percent(invested, total))

                .otherAmount(other)
                .otherRatioPercent(percent(other, total))

                // 투자 자산 내 비율(분모: invested)
                .stockRatioPercent(percent(stock, invested))
                .bitcoinRatioPercent(percent(bitcoin, invested))
                .bondRatioPercent(percent(bond, invested))
                .etfRatioPercent(percent(etf, invested))
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0)
            return BigDecimal.ZERO.setScale(PCT_SCALE, RM);
        return nz(part).multiply(BigDecimal.valueOf(100))
                .divide(base, PCT_SCALE, RM);
    }
}
