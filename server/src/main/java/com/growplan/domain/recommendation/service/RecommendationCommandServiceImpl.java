package com.growplan.domain.recommendation.service;

import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.asset.repository.AssetPortfolioRepository;
import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.converter.RecommendationConverter;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;
import com.growplan.domain.recommendation.entity.InvestmentDesign;
import com.growplan.domain.recommendation.repository.InvestmentDesignRepository;
import com.growplan.domain.recommendation.repository.RecommendationRepository;
import com.growplan.global.common.enums.*;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.handler.AssetException;
import com.growplan.global.error.exception.handler.InvestmentDesignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.growplan.domain.recommendation.converter.RecommendationConverter.toResponse;
import static java.lang.Math.pow;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationCommandServiceImpl implements RecommendationCommandService {

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int MONEY_SCALE = 0; // 만원 단위 반올림
    private static final int PCT_SCALE = 1;   // 퍼센트 소수 1자리
    private static final double DEFAULT_CASH_RATIO = 0.10;

    private final InvestmentDesignRepository investmentDesignRepository;
    private final AssetPortfolioRepository assetPortfolioRepository;
    private final WebClient recommendWebClient;


    @Override
    public void designInvestmentPlan(Member member, RecommendationRequestDTO.InvestmentDesignRequest request) {

        InvestmentDesign design = investmentDesignRepository.findByMember(member)
                .orElseGet(() -> RecommendationConverter.toCreateDesign(member, request));

        investmentDesignRepository.save(design);
    }

    private record LabelAndYears(String label, double years) {}
    private record TimelineSpec(String title, int pointCount, List<LabelAndYears> labels) {}

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponseDTO.AIPortfolioResponse getAiRecommendedPortfolio(Member member) {

        InvestmentDesign design = investmentDesignRepository.findByMember(member)
                .orElseThrow(() -> new InvestmentDesignException(ErrorStatus.INVESTMENT_DESIGN_NOT_FOUND));
        AssetPortfolio portfolio = assetPortfolioRepository.findByMember(member)
                .orElseThrow(() -> new AssetException(ErrorStatus.ASSET_NOT_FOUND));

        BigDecimal totalAmount = portfolio.getTotalAmount() == null ? BigDecimal.ZERO : portfolio.getTotalAmount();

        int monthlyIncome = incomeRangeToMonthly(design.getIncomeRange());
        int emergencyFund = calcEmergencyFund(monthlyIncome);

        // 총점(성향/기간/목적/목표수익률-대응) → 안전/투자 큰 비율
        int totalScore = 0;
        totalScore += scoreByPropensity(design.getPropensity());
        totalScore += scoreByInvestmentPeriod(design.getInvestmentPeriod());
        totalScore += scoreByPurpose(design.getInvestmentPurpose());
        totalScore += scoreByTargetReturn(design.getProfitRange());

        // 파이썬 로직
        double cash = DEFAULT_CASH_RATIO;
        double[] safeInvest = safeInvestSplit(totalScore); // [safe, invest]
        double residual = Math.max(0.0, 1.0 - cash);
        double safeRatio = residual * safeInvest[0];
        double investRatio = residual * safeInvest[1];

        double[] depSav = depositSavingsSplit(safeRatio, totalAmount, monthlyIncome);
        double depositRatio = depSav[0];
        double savingsRatio = depSav[1];

        // 정규화
        double sum = cash + depositRatio + savingsRatio + investRatio;
        if (sum > 0) {
            cash /= sum; depositRatio /= sum; savingsRatio /= sum; investRatio /= sum;
        }

        // 금액 계산
//        BigDecimal cashAmt = pctOf(totalAmount, cash);
//        BigDecimal depAmt  = pctOf(totalAmount, depositRatio);
//        BigDecimal savAmt  = pctOf(totalAmount, savingsRatio);
//        BigDecimal invAmt  = pctOf(totalAmount, investRatio);

        return toResponse(totalAmount, cash, depositRatio, savingsRatio, investRatio, design.getIncomeRange(),
                design.getInvestmentPeriod(),
                design.getPropensity());
    }

    @Override
    public RecommendationResponseDTO.RecommendApiResult getExternalInvestment(Member member) {

        InvestmentDesign design = investmentDesignRepository.findByMember(member)
                .orElseThrow(() -> new InvestmentDesignException(ErrorStatus.INVESTMENT_DESIGN_NOT_FOUND));
        AssetPortfolio portfolio = assetPortfolioRepository.findByMember(member)
                .orElseThrow(() -> new AssetException(ErrorStatus.ASSET_NOT_FOUND));
        BigDecimal base = nvl(portfolio.getTotalAmount()).setScale(MONEY_SCALE, RM);

        int riskLevel = toRiskLevel(design.getPropensity());

        // 2) 요청 바디 구성 (고정 + risk_level만 DB에서)
        RecommendationRequestDTO.ExternalRecommendRequest body = new RecommendationRequestDTO.ExternalRecommendRequest();
        body.setAssets(List.of("SPY", "QQQM", "277630.KS", "272910.KS", "IMTB"));
        body.setRisk_level(riskLevel);
        body.setLookback_years(3);
        body.setRf(0.0);
        body.setPoints(10);

        // 3) 외부 API POST 호출
//        try {
//            return recommendWebClient.post()
//                    .uri("/api/recommend")
//                    .bodyValue(body)
//                    .retrieve()
//                    .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
//                            .flatMap(msg -> Mono.error(new IllegalStateException(
//                                    "Recommend API error: HTTP " + resp.statusCode().value() + " - " + msg))))
//                    .bodyToMono(RecommendationResponseDTO.RecommendApiResult.class)
//                    .timeout(Duration.ofSeconds(10))
//                    .block();
//
//        } catch (WebClientResponseException e) {
//            throw new IllegalStateException("Recommend API HTTP " + e.getRawStatusCode() + " - " + e.getResponseBodyAsString(), e);
//        } catch (Exception e) {
//            throw new IllegalStateException("Recommend API call failed", e);
//        }

        RecommendationResponseDTO.RecommendApiResult rec =
                recommendWebClient.post()
                        .uri("/api/recommend")
                        .bodyValue(buildExternalBody(design.getPropensity()))
                        .retrieve()
                        .onStatus(org.springframework.http.HttpStatusCode::isError, resp ->
                                resp.bodyToMono(String.class).flatMap(msg ->
                                        reactor.core.publisher.Mono.error(new IllegalStateException(
                                                "Recommend API error: HTTP " + resp.statusCode().value() + " - " + msg))))
                        .bodyToMono(RecommendationResponseDTO.RecommendApiResult.class)
                        .timeout(Duration.ofSeconds(10))
                        .block();
        if (rec == null) throw new IllegalStateException("Recommend API returned null");

        // 3) 타임라인 스펙(스크린샷 가이드 그대로)
        TimelineSpec spec = buildTimeline(design.getInvestmentPeriod());

        // 4) 연복리로 미래값 계산: FV = base * (1+r)^t
        double r = rec.getAnnual_return(); // 예: 0.1764
        List<RecommendationResponseDTO.ForecastPoint> pts = new ArrayList<>();
        for (LabelAndYears ly : spec.labels()) {
            BigDecimal fv = base.multiply(BigDecimal.valueOf(pow(1.0 + r, ly.years())))
                    .setScale(MONEY_SCALE, RM);
            pts.add(RecommendationResponseDTO.ForecastPoint.builder()
                    .label(ly.label())
                    .years(ly.years())
                    .amount(fv)
                    .build());
        }

        rec.setEtfList(buildDummyAssetCards());
        rec.setReasonText("String");
        rec.setHorizon_title(spec.title());
        rec.setPoint_count(spec.pointCount());
        rec.setBase_amount(base);
        rec.setPeriod(design.getInvestmentPeriod());
        rec.setForecast_points(pts);

        return rec;
    }

//    private static BigDecimal pctOf(BigDecimal total, double ratio01) {
//        return total.multiply(BigDecimal.valueOf(ratio01))
//                .setScale(MONEY_SCALE, RM);
//    }
//    private static BigDecimal toPct(double ratio01) {
//        return BigDecimal.valueOf(ratio01 * 100.0).setScale(PCT_SCALE, RM);
//    }

    private static int scoreByPropensity(Propensity p) {
        if (p == null) return 0;
        return switch (p) {
            case STABLE -> 0;     // 안정형
            case SAFETY -> 10;    // 보통형
            case ACTIVE -> 20;    // 위험중립형
            case AGGRESSIVE -> 30; // 적극투자형
            case AGGRESSIVE_INVESTMENT -> 40; // 적극 공격형
        };
    }

    private static int scoreByInvestmentPeriod(InvestmentPeriod period) {
        if (period == null) return 0;

        // 파이썬 로직 매핑:
        // 1년~3년 미만(+10), 3~5년(+20), 5년 이상(+30)
        return switch (period) {
            // 3년 이내
            case UNDER_6_MONTHS,
                    UNDER_1_YEAR,
                    UNDER_2_YEARS,
                    UNDER_3_YEARS -> 10;

            // 5년 이내
            case UNDER_5_YEARS -> 20;

            // 10년 이내/이상(장기) → 파이썬 기준 "5년 이상" 버킷과 동일
            case UNDER_10_YEARS,
                    OVER_10_YEARS -> 30;
        };
    }

    private static int scoreByPurpose(InvestmentPurpose purpose) {
        if (purpose == null) return 0;
        return switch (purpose) {
            case CAR_PURCHASE -> 10;
            case MARRIAGE, HOME_OWNERSHIP -> 20;
            default -> 0; // SAVINGS, TRAVEL, SELF_DEVELOPMENT 등
        };
    }

    private static int scoreByTargetReturn(ProfitRange pr) {
        if (pr == null) return 0;
        return switch (pr) {
            case BELOW_500, BETWEEN_500_1000 -> 10;     // ~10%
            case BETWEEN_1000_3000 -> 20;               // 10~15%
            case BETWEEN_3000_5000, BETWEEN_5000_10000, ABOVE_10000 -> 30; // 15%+
        };
    }

    private static double[] safeInvestSplit(int score) {
        double safe, invest;
        if (score <= 30) { safe = 0.80; invest = 0.20; }
        else if (score <= 60) { safe = 0.60; invest = 0.40; }
        else if (score <= 90) { safe = 0.40; invest = 0.60; }
        else { safe = 0.20; invest = 0.80; }
        return new double[]{safe, invest};
    }

    private static double[] depositSavingsSplit(double safeRatio, BigDecimal currentAsset, int monthlyIncome) {
        double runway = monthlyIncome <= 0 ? 0.0 :
                currentAsset.doubleValue() / (double) monthlyIncome;

        double savingsShare; // 적금 비중
        if (runway >= 6)      savingsShare = 0.30;
        else if (runway >= 3) savingsShare = 0.50;
        else if (runway >= 1) savingsShare = 0.65;
        else                  savingsShare = 0.80;

        double depositShare = 1.0 - savingsShare;
        return new double[]{ safeRatio * depositShare, safeRatio * savingsShare };
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

    private static int toRiskLevel(Propensity p) {
        if (p == null) return 3;
        return switch (p) {
            case STABLE -> 1;
            case SAFETY -> 2;
            case ACTIVE -> 3;
            case AGGRESSIVE -> 4;
            case AGGRESSIVE_INVESTMENT -> 5;
        };
    }
    private RecommendationRequestDTO.ExternalRecommendRequest buildExternalBody(com.growplan.global.common.enums.Propensity p) {
        int riskLevel = toRiskLevel(p);
        var body = new RecommendationRequestDTO.ExternalRecommendRequest();
        body.setAssets(java.util.List.of("SPY","QQQM","277630.KS","272910.KS","IMTB"));
        body.setRisk_level(riskLevel);
        body.setLookback_years(3);
        body.setRf(0.0);
        body.setPoints(10);
        return body;
    }

    private static TimelineSpec buildTimeline(InvestmentPeriod p) {
        if (p == null) {
            return new TimelineSpec("6개월", 3, Arrays.asList(
                    new LabelAndYears("현재", 0.0),
                    new LabelAndYears("3개월 후", 0.25),
                    new LabelAndYears("6개월 후", 0.50)
            ));
        }
        return switch (p) {
            case UNDER_6_MONTHS -> new TimelineSpec("6개월", 3, Arrays.asList(
                    new LabelAndYears("현재", 0.0),
                    new LabelAndYears("3개월 후", 0.25),
                    new LabelAndYears("6개월 후", 0.50)
            ));
            case UNDER_1_YEAR -> new TimelineSpec("1년", 3, Arrays.asList(
                    new LabelAndYears("현재", 0.0),
                    new LabelAndYears("6개월 후", 0.50),
                    new LabelAndYears("1년 후", 1.0)
            ));
            case UNDER_2_YEARS -> new TimelineSpec("2년", 3, Arrays.asList(
                    new LabelAndYears("현재", 0.0),
                    new LabelAndYears("1년 후", 1.0),
                    new LabelAndYears("2년 후", 2.0)
            ));
            case UNDER_3_YEARS -> new TimelineSpec("3년", 4, Arrays.asList(
                    new LabelAndYears("현재", 0.0),
                    new LabelAndYears("1년 후", 1.0),
                    new LabelAndYears("2년 후", 2.0),
                    new LabelAndYears("3년 후", 3.0)
            ));
            case UNDER_5_YEARS -> new TimelineSpec("5년", 6, Arrays.asList(
                    new LabelAndYears("현재", 0.0),
                    new LabelAndYears("1년 후", 1.0),
                    new LabelAndYears("2년 후", 2.0),
                    new LabelAndYears("3년 후", 3.0),
                    new LabelAndYears("4년 후", 4.0),
                    new LabelAndYears("5년 후", 5.0)
            ));
            case UNDER_10_YEARS, OVER_10_YEARS -> new TimelineSpec("10년", 6, Arrays.asList(
                    new LabelAndYears("현재", 0.0),
                    new LabelAndYears("2년 후", 2.0),
                    new LabelAndYears("4년 후", 4.0),
                    new LabelAndYears("6년 후", 6.0),
                    new LabelAndYears("8년 후", 8.0),
                    new LabelAndYears("10년 후", 10.0)
            ));
        };
    }

    private static List<RecommendationResponseDTO.AssetCard> buildDummyAssetCards() {
        return java.util.Arrays.asList(
                RecommendationResponseDTO.AssetCard.builder()
                        .symbol("QQQM")
                        .etf_name("Invesco NASDAQ 100 ETF")
                        .price(new BigDecimal("234.81"))
                        .currency("USD")
                        .day_change_pct(-1.17)
                        .build(),
                RecommendationResponseDTO.AssetCard.builder()
                        .symbol("277630.KS")
                        .etf_name("TIGER 코스피")
                        .price(new BigDecimal("33090"))
                        .currency("KRW")
                        .day_change_pct(-0.17)
                        .build(),
                RecommendationResponseDTO.AssetCard.builder()
                        .symbol("QQQM")
                        .etf_name("ACE 중장기국공채액티브")
                        .price(new BigDecimal("109495"))
                        .currency("KRW")
                        .day_change_pct(-0.03)
                        .build()
        );
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
