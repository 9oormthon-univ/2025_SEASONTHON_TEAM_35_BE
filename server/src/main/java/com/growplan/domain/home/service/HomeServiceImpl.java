package com.growplan.domain.home.service;

import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.asset.repository.AssetPortfolioRepository;
import com.growplan.domain.asset.service.AssetQueryService;
import com.growplan.domain.goal.dto.GoalResponseDTO;
import com.growplan.domain.goal.service.GoalCommandService;
import com.growplan.domain.home.dto.HomeResponseDTO;
import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;
import com.growplan.domain.recommendation.entity.InvestmentDesign;
import com.growplan.domain.recommendation.service.RecommendationCommandService;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.handler.AssetException;
import com.growplan.global.error.exception.handler.InvestmentDesignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final AssetQueryService assetQueryService;
    private final GoalCommandService goalCommandService;
    private final RecommendationCommandService recommendationCommandService;

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int MONEY_SCALE = 0;

    public HomeResponseDTO.OverviewDTO getOverview(Member member) {

        AssetResponseDTO.RegisterAssetResponseDTO asset = assetQueryService.getAssetSummary(member);

        BigDecimal total   = nvl(asset.getTotalAmount()).setScale(MONEY_SCALE, RM);
        BigDecimal cash    = nvl(asset.getCashAmount());
        BigDecimal deposit = nvl(asset.getDepositAmount());
        BigDecimal savings = nvl(asset.getSavingsAmount());
        BigDecimal invest  = nvl(asset.getInvestedAmount());
        BigDecimal other   = nvl(asset.getOtherAmount());

        int cashRatio    = ratio(cash, total);
        int depositRatio = ratio(deposit, total);
        int savingRatio  = ratio(savings, total);
        int investRatio  = ratio(invest, total);
        int etcRatio     = ratio(other, total);


        GoalResponseDTO.GoalAnalysisResponseDTO goal = goalCommandService.analyzeGoal(member);
        BigDecimal targetAmount = nvl(goal.getTargetAmount()).setScale(MONEY_SCALE, RM);

        int achievementRate = achievement(total, targetAmount);

        HomeResponseDTO.InvestmentForecastDTO forecastDto = null;
        try {
            RecommendationResponseDTO.RecommendApiResult rec =
                    recommendationCommandService.getExternalInvestment(member);

            if (rec != null) {
                forecastDto = HomeResponseDTO.InvestmentForecastDTO.builder()
                        .horizonTitle(rec.getHorizonTitle())
                        .pointCount(rec.getPointCount())
                        .currentAmount(rec.getCurrentAmount())
                        .period(rec.getPeriod())
                        .forecastPoints(rec.getForecast_points())
                        .build();
            }
        } catch (Exception ignore) { /* 홈은 실패해도 나머지 정보 제공 */ }

        return HomeResponseDTO.OverviewDTO.builder()
                .name(safeName(member))
                .totalAsset(total)
                .cashRatio(cashRatio)
                .depositRatio(depositRatio)
                .savingRatio(savingRatio)
                .investRatio(investRatio)
                .etcRatio(etcRatio)
                .targetAmount(targetAmount)
                .investmentForecast(forecastDto)
                .achievementRate(achievementRate)
                .build();
    }

    private String safeName(Member m) {
        try { return m.getName(); } catch (Exception e) { return "회원"; }
    }

    private int ratio(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return part.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RM).intValue();
    }

    private int achievement(BigDecimal total, BigDecimal target) {
        if (target.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return total.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RM).intValue();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
