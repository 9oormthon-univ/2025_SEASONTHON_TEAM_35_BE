package com.growplan.domain.recommendation.converter;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;
import com.growplan.domain.recommendation.entity.InvestmentDesign;
import com.growplan.global.common.enums.IncomeRange;
import com.growplan.global.common.enums.InvestmentPeriod;
import com.growplan.global.common.enums.Propensity;
import com.growplan.global.common.enums.SavingRange;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RecommendationConverter {

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int MONEY_SCALE = 0; // 만원 단위
    private static final int PCT_SCALE   = 1; // 퍼센트 소수 1자리

    public static InvestmentDesign toCreateDesign(Member member, RecommendationRequestDTO.InvestmentDesignRequest request) {

        return InvestmentDesign.builder()
                .savingRange(request.getSavingRange())
                .incomeRange(request.getIncomeRange())
                .profitRange(request.getProfitRange())
                .investmentPeriod(request.getInvestmentPeriod())
                .propensity(request.getPropensity())
                .investmentPurpose(request.getInvestmentPurpose())
                .emergencyFund(Boolean.TRUE.equals(request.getEmergencyFund()))
                .member(member)
                .build();
    }

    public static RecommendationResponseDTO.AIPortfolioResponse toResponse(
            BigDecimal totalAmount,
            double cashRatio, double depositRatio, double savingsRatio, double investmentRatio,
            String recommendationMessage,
            SavingRange savingRange,
            InvestmentPeriod investmentPeriod,
            Propensity propensity
    ) {
        BigDecimal total = nvl(totalAmount).setScale(MONEY_SCALE, RM);

        BigDecimal cashAmt = amountOf(total, cashRatio);
        BigDecimal depAmt  = amountOf(total, depositRatio);
        BigDecimal savAmt  = amountOf(total, savingsRatio);
        BigDecimal invAmt  = amountOf(total, investmentRatio);

        return RecommendationResponseDTO.AIPortfolioResponse.builder()
                .cashAmount(cashAmt)
                .depositAmount(depAmt)
                .savingsAmount(savAmt)
                .investmentAmount(invAmt)
                .cashRatioPct(pct(cashRatio))
                .depositRatioPct(pct(depositRatio))
                .savingsRatioPct(pct(savingsRatio))
                .investmentRatioPct(pct(investmentRatio))
                .recommendationMessage(recommendationMessage)
                .totalAmount(total)
                .savingRange(savingRange)
                .investmentPeriod(investmentPeriod)
                .propensity(propensity)
                .build();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal amountOf(BigDecimal total, double ratio01) {
        return total.multiply(BigDecimal.valueOf(ratio01)).setScale(MONEY_SCALE, RM);
    }

    private static BigDecimal pct(double ratio01) {
        return BigDecimal.valueOf(ratio01 * 100).setScale(PCT_SCALE, RM);
    }


}
