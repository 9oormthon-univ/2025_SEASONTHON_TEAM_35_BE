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

    private static final BigDecimal TOTAL = new BigDecimal("12000000");

    // 총자산 내 비중(%)
    private static final int CASH_PCT    = 20;
    private static final int DEPOSIT_PCT = 25;
    private static final int SAVINGS_PCT = 10;
    private static final int INVEST_PCT  = 40;
    // OTHER는 잔여(= 100 - 위 합계)로 계산

    // 투자자산 내 비중(%)
    private static final int STOCK_PCT   = 50;
    private static final int BTC_PCT     = 10;
    private static final int BOND_PCT    = 20;
    private static final int ETF_PCT     = 20;

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

    public static AssetDummySpec dummySpec() {
        BigDecimal cash     = pctOf(TOTAL, CASH_PCT);
        BigDecimal deposit  = pctOf(TOTAL, DEPOSIT_PCT);
        BigDecimal savings  = pctOf(TOTAL, SAVINGS_PCT);
        BigDecimal invested = pctOf(TOTAL, INVEST_PCT);

        BigDecimal other = clampNonNegative(
                TOTAL.subtract(cash).subtract(deposit).subtract(savings).subtract(invested)
        );

        BigDecimal stock   = pctOf(invested, STOCK_PCT);
        BigDecimal bitcoin = pctOf(invested, BTC_PCT);
        BigDecimal bond    = pctOf(invested, BOND_PCT);
        BigDecimal etf     = pctOf(invested, ETF_PCT);

        return new AssetDummySpec(TOTAL, cash, deposit, savings, invested, other, stock, bitcoin, bond, etf);
    }

    public static record AssetDummySpec(
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
    ) {}

    public static AssetResponseDTO.RegisterAssetResponseDTO toRegisterAssetResponse(AssetDummySpec s) {
        return AssetResponseDTO.RegisterAssetResponseDTO.builder()
                .totalAmount(s.total)

                .cashAmount(s.cash)
                .cashRatioPercent(percent(s.cash, s.total))

                .depositAmount(s.deposit)
                .depositRatioPercent(percent(s.deposit, s.total))

                .savingsAmount(s.savings)
                .savingsRatioPercent(percent(s.savings, s.total))

                .investedAmount(s.invested)
                .investedRatioPercent(percent(s.invested, s.total))

                .otherAmount(s.other)
                .otherRatioPercent(percent(s.other, s.total))

                .stockRatioPercent(percent(s.stock, s.invested))
                .bitcoinRatioPercent(percent(s.bitcoin, s.invested))
                .bondRatioPercent(percent(s.bond, s.invested))
                .etfRatioPercent(percent(s.etf, s.invested))
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

    private static BigDecimal pctOf(BigDecimal base, int pct) {
        // base * (pct/100)
        return nz(base).multiply(BigDecimal.valueOf(pct))
                .divide(BigDecimal.valueOf(100), 0, RM);
    }

    private static BigDecimal clampNonNegative(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO;
        return v.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : v;
    }

//    public static AssetResponseDTO.RegisterAssetResponseDTO toRegisterAssetResponse (
//            BigDecimal total,
//            BigDecimal cash,
//            BigDecimal deposit,
//            BigDecimal savings,
//            BigDecimal invested,
//            BigDecimal other,
//            BigDecimal stock,
//            BigDecimal bitcoin,
//            BigDecimal bond,
//            BigDecimal etf
//    ) {
//        total = nz(total);
//        cash = nz(cash);
//        invested = nz(invested);
//        other = nz(other);
//
//        return AssetResponseDTO.RegisterAssetResponseDTO.builder()
//                .totalAmount(total)
//
//                .cashAmount(cash)
//                .cashRatioPercent(percent(cash, total))
//
//                .depositAmount(deposit)
//                .depositRatioPercent(percent(deposit, total))
//
//                .savingsAmount(savings)
//                .savingsRatioPercent(percent(savings, total))
//
//                .investedAmount(invested)
//                .investedRatioPercent(percent(invested, total))
//
//                .otherAmount(other)
//                .otherRatioPercent(percent(other, total))
//
//                // 투자 자산 내 비율(분모: invested)
//                .stockRatioPercent(percent(stock, invested))
//                .bitcoinRatioPercent(percent(bitcoin, invested))
//                .bondRatioPercent(percent(bond, invested))
//                .etfRatioPercent(percent(etf, invested))
//                .build();
//    }

//    private static BigDecimal nz(BigDecimal v) {
//        return v == null ? BigDecimal.ZERO : v;
//    }
//
//    private static BigDecimal percent(BigDecimal part, BigDecimal base) {
//        if (base == null || base.compareTo(BigDecimal.ZERO) == 0)
//            return BigDecimal.ZERO.setScale(PCT_SCALE, RM);
//        return nz(part).multiply(BigDecimal.valueOf(100))
//                .divide(base, PCT_SCALE, RM);
//    }
}
