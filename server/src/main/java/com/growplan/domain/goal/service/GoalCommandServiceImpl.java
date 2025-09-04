package com.growplan.domain.goal.service;

import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.asset.repository.AssetPortfolioRepository;
import com.growplan.domain.goal.converter.GoalConverter;
import com.growplan.domain.goal.dto.GoalResponseDTO;
import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.entity.InvestmentDesign;
import com.growplan.domain.recommendation.repository.InvestmentDesignRepository;
import com.growplan.global.common.enums.*;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.handler.AssetException;
import com.growplan.global.error.exception.handler.InvestmentDesignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GoalCommandServiceImpl implements GoalCommandService {

    // target = SavingRange(월저축가능액 대표값) × InvestmentPeriod(기간-개월) × Purpose계수

    private final InvestmentDesignRepository investmentDesignRepository;
    private final AssetPortfolioRepository assetPortfolioRepository;

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int MONEY_SCALE = 0; // 만원 단위
    private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");
    private static final BigDecimal TARGET_UPLIFT_RATE = new BigDecimal("0.25");

    @Override
    public GoalResponseDTO.GoalAnalysisResponseDTO analyzeGoal(Member member) {

        InvestmentDesign design = investmentDesignRepository.findByMember(member)
                .orElseThrow(() -> new InvestmentDesignException(ErrorStatus.INVESTMENT_DESIGN_NOT_FOUND));
        AssetPortfolio portfolio = assetPortfolioRepository.findByMember(member)
                .orElseThrow(() -> new AssetException(ErrorStatus.ASSET_NOT_FOUND));

        BigDecimal totalAmount = portfolio.getTotalAmount() == null
                ? BigDecimal.ZERO : portfolio.getTotalAmount();

        int monthlyIncomeMan = incomeRangeToMonthly(design.getIncomeRange());      // 만원/월
        int monthlySavingMan = savingRangeToMonthly(design.getSavingRange());     // 만원/월
        int emergencyFundMan = calcEmergencyFund(monthlyIncomeMan);

        BigDecimal monthlyIncome = BigDecimal.valueOf(monthlyIncomeMan).multiply(TEN_THOUSAND);   // 원/월
        BigDecimal monthlySavingCapacity = BigDecimal.valueOf(monthlySavingMan).multiply(TEN_THOUSAND); // 원/월
        BigDecimal emergencyFund = BigDecimal.valueOf(emergencyFundMan).multiply(TEN_THOUSAND);   // 원

        // TODO: 목표 금액 임시 생성
        int months = periodToMonths(design.getInvestmentPeriod());
        double purposeFactor = purposeFactor(design.getInvestmentPurpose());

        BigDecimal planSavingBase = monthlySavingCapacity
                .multiply(BigDecimal.valueOf(months))
                .multiply(BigDecimal.valueOf(purposeFactor));

        BigDecimal planSaving = planSavingBase
                .multiply(BigDecimal.ONE.add(TARGET_UPLIFT_RATE))
                .setScale(MONEY_SCALE, RM);

        BigDecimal targetAmount = totalAmount.add(planSaving).setScale(0, RM);

        // 월 저축액
        BigDecimal gap = targetAmount.subtract(totalAmount);
        int needPerMonth = months <= 0 ? 0 :
                gap.divide(BigDecimal.valueOf(months), 0, RM).intValue();

        String feasibilityText = evaluateFeasibility(
                needPerMonth, monthlySavingCapacity.intValue(), monthlyIncome.intValue(), design.getPropensity());

        int achievementRate = achievement(totalAmount, targetAmount);

        return GoalConverter.toAnalysisResponse(
                totalAmount.setScale(MONEY_SCALE, RM),
                targetAmount,
                emergencyFund.intValue(),
                design.getInvestmentPurpose(),
                needPerMonth,
                feasibilityText,
                achievementRate
        );
    }

    private int achievement(BigDecimal total, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return total.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RM).intValue();
    }

    private static String evaluateFeasibility(int needPerMonth, int capacity, int income, Propensity p) {
        double capFactor, incomePct;
        switch (p == null ? Propensity.ACTIVE : p) {
            case STABLE -> { capFactor = 1.0; incomePct = 0.30; }
            case SAFETY -> { capFactor = 1.2; incomePct = 0.40; }
            case AGGRESSIVE -> { capFactor = 1.7; incomePct = 0.60; }
            case AGGRESSIVE_INVESTMENT -> { capFactor = 2.0; incomePct = 0.70; }
            default       -> { capFactor = 1.5; incomePct = 0.50; } // ACTIVE
        }
        int capLimit = (int)Math.round(capacity * capFactor);
        int incomeLimit = (int)Math.round(income * incomePct);
        boolean ok = needPerMonth <= Math.min(capLimit, incomeLimit);

        return ok
                ? "현재 소득과 투자 성향을 고려했을 때 무리가 없는 수준입니다."
                : "현재 소득과 투자 성향을 고려했을 때 무리가 있습니다.";
    }

    private static int calcEmergencyFund(int income) {
        if (income <= 200) return 500;
        if (income <= 300) return 800;
        if (income <= 500) return 1500;
        return 3000;
    }

    private static int incomeRangeToMonthly(IncomeRange r) {
        if (r == null) return 0;
        return switch (r) {
            case BELOW_100 -> 200;
            case BETWEEN_100_200 -> 150;
            case BETWEEN_200_300 -> 250;
            case BETWEEN_300_500 -> 400;
            case ABOVE_500 -> 600;
        };
    }

    private static int savingRangeToMonthly(SavingRange s) {
        if (s == null) return 0;
        // 구간의 "대표값" 가정(만원)
        return switch (s) {
            case BELOW_10 -> 5;
            case BETWEEN_10_50 -> 30;
            case BETWEEN_50_100 -> 75;
            case BETWEEN_100_200 -> 150;
            case ABOVE_200 -> 250;
        };
    }

    private static int periodToMonths(InvestmentPeriod p) {
        if (p == null) return 12;
        return switch (p) {
            case UNDER_6_MONTHS -> 6;
            case UNDER_1_YEAR -> 12;
            case UNDER_2_YEARS -> 24;
            case UNDER_3_YEARS -> 36;
            case UNDER_5_YEARS -> 60;
            case UNDER_10_YEARS -> 120;
            case OVER_10_YEARS -> 180; // 계산용 상한
        };
    }

    private static double purposeFactor(InvestmentPurpose purpose) {
        if (purpose == null) return 1.0;
        return switch (purpose) {
            case SAVINGS -> 0.8;
            case TRAVEL, SELF_DEVELOPMENT -> 0.6;
            case CAR_PURCHASE -> 1.0;
            case MARRIAGE -> 1.2;
            case HOME_OWNERSHIP -> 1.5;
        };
    }
}
