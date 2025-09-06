package com.growplan.domain.recommendation.service;

import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.asset.repository.AssetPortfolioRepository;
import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.converter.RecommendationConverter;
import com.growplan.domain.recommendation.dto.GeminiRequest;
import com.growplan.domain.recommendation.dto.GeminiResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class RecommendationCommandServiceImpl implements RecommendationCommandService {

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int MONEY_SCALE = 0; // 만원 단위 반올림
    private static final double DEFAULT_CASH_RATIO = 0.10;

    private final InvestmentDesignRepository investmentDesignRepository;
    private final AssetPortfolioRepository assetPortfolioRepository;

    private final WebClient recommendWebClient;
    private final WebClient geminiWebClient;
    @Value("${gemini.model}")
    private String geminiModel;

    @Override
    public void designInvestmentPlan(Member member, RecommendationRequestDTO.InvestmentDesignRequest request) {

        InvestmentDesign design = investmentDesignRepository.findByMember(member)
                .orElseGet(() -> RecommendationConverter.toCreateDesign(member, request));

        investmentDesignRepository.save(design);
    }

    private record LabelAndYears(String label, double years) {}
    private record TimelineSpec(String title, int pointCount, List<LabelAndYears> labels) {}

    @Override
    public RecommendationResponseDTO.AIPortfolioResponse getAiRecommendedPortfolio(Member member) {

        InvestmentDesign design = investmentDesignRepository.findByMemberOrNull(member);
        AssetPortfolio portfolio = assetPortfolioRepository.findByMember(member)
                .orElseThrow(() -> new AssetException(ErrorStatus.ASSET_NOT_FOUND));

        if (design == null) return null;

        BigDecimal totalAmount = portfolio.getTotalAmount() == null ? BigDecimal.ZERO : portfolio.getTotalAmount();

        int monthlyIncome = incomeRangeToMonthly(design.getIncomeRange());

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

        String recommendationMessage =
                generateAllocationMessageWithGemini(totalAmount, design, cash, depositRatio, savingsRatio, investRatio);
       log.info("💡 [Gemini] 분배 비율 해설: {}", recommendationMessage);
//        if (recommendationMessage == null || recommendationMessage.isBlank()) {
//            recommendationMessage = buildRecommendationMessage(design); // 폴백
//        }

//        String recommendationMessage = buildRecommendationMessage(design);

        // 금액 계산
//        BigDecimal cashAmt = pctOf(totalAmount, cash);
//        BigDecimal depAmt  = pctOf(totalAmount, depositRatio);
//        BigDecimal savAmt  = pctOf(totalAmount, savingsRatio);
//        BigDecimal invAmt  = pctOf(totalAmount, investRatio);

        return toResponse(totalAmount, cash, depositRatio, savingsRatio, investRatio, recommendationMessage,
                design.getSavingRange(),
                design.getInvestmentPeriod(),
                design.getPropensity());
    }

    @Override
    public RecommendationResponseDTO.RecommendApiResult getExternalInvestment(Member member)    {

        InvestmentDesign design = investmentDesignRepository.findByMemberOrNull(member);
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
                        .timeout(Duration.ofSeconds(100000))
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

        String reason = generateReasonTextWithGemini(rec);

        rec.setEtfList(buildDummyAssetCards());
        rec.setReasonText(reason != null ? reason : "포트폴리오 지표를 기반으로 한 해설 생성에 일시적으로 실패했습니다.");
        rec.setHorizonTitle(spec.title());
        rec.setPointCount(spec.pointCount());
        rec.setCurrentAmount(base);
        rec.setPeriod(design.getInvestmentPeriod());
        rec.setForecast_points(pts);

        return rec;
    }


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
                        .etfName("Invesco NASDAQ 100 ETF")
                        .price(new BigDecimal("234.81"))
                        .currency("USD")
                        .dayChangePct(-1.17)
                        .build(),
                RecommendationResponseDTO.AssetCard.builder()
                        .symbol("277630.KS")
                        .etfName("TIGER 코스피")
                        .price(new BigDecimal("33090"))
                        .currency("KRW")
                        .dayChangePct(-0.17)
                        .build(),
                RecommendationResponseDTO.AssetCard.builder()
                        .symbol("QQQM")
                        .etfName("ACE 중장기국공채액티브")
                        .price(new BigDecimal("109495"))
                        .currency("KRW")
                        .dayChangePct(-0.03)
                        .build()
        );
    }

    // ===== 분배 비율 설명을 Gemini로 생성 =====
    private String generateAllocationMessageWithGemini(
            BigDecimal totalAmount,
            InvestmentDesign d,
            double cashRatio, double depositRatio, double savingsRatio, double investRatio
    ) {
        String summary = buildAllocationSummary(totalAmount, d, cashRatio, depositRatio, savingsRatio, investRatio);
        String prompt  = buildAllocationPrompt(summary);

        var req = GeminiRequest.builder()
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(List.of(GeminiRequest.Part.builder().text(prompt).build()))
                                .build()
                ))
                .build();

        try {
            var resp = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/models/" + geminiModel + ":generateContent").build())
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                            .flatMap(msg -> Mono.error(new IllegalStateException("[Gemini] HTTP "
                                    + r.statusCode().value() + " - " + msg))))
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(12))
                    .block();

            if (resp == null || resp.getCandidates() == null || resp.getCandidates().isEmpty()) return null;
            var cand = resp.getCandidates().get(0);
            if (cand.getContent() == null || cand.getContent().getParts() == null || cand.getContent().getParts().isEmpty()) return null;

            return cand.getContent().getParts().get(0).getText();
        } catch (Exception e) {
            return null;
        }
    }

    // ===== 요약 문자열 생성 =====
    private String buildAllocationSummary(
            BigDecimal totalAmount,
            InvestmentDesign d,
            double cashR, double depR, double savR, double invR
    ) {
        var pct0 = new java.text.DecimalFormat("0");  // 정수 %
        String totalStr = formatMoney(totalAmount);

        String incomeStr   = toKoIncomeRange(d.getIncomeRange());      // 예: "200~300만원"
        String savingStr   = toKoSavingRange(d.getSavingRange());      // 예: "50~100만원"
        String targetStr   = toKoProfitRange(d.getProfitRange());      // 예: "10~15%"
        String periodStr   = toKoPeriod(d.getInvestmentPeriod());      // 예: "2년 이내"
        String propStr     = toKoPropensity(d.getPropensity());        // 예: "안정형"
        String purposeStr  = toKoPurpose(d.getInvestmentPurpose());    // 예: "저축"
        String efStr       = Boolean.TRUE.equals(d.getEmergencyFund()) ? "필요" : "불필요";

        String ratios = "추천 비율(%)  현금: " + pct0.format(cashR * 100)
                + "% / 예금: " + pct0.format(depR * 100)
                + "% / 적금: " + pct0.format(savR * 100)
                + "% / 투자: " + pct0.format(invR * 100);

        return """
[개인 정보]
총 자산 : %s
월 소득 : %s
월 저축 금액 : %s
목표 수익률 : %s
목표 시점 : %s
투자 성향 : %s
투자 목표 : %s
비상금 : %s

[자산 분배 비율]
%s
""".formatted(totalStr, incomeStr, savingStr, targetStr, periodStr, propStr, purposeStr, efStr, ratios);
    }

    // ===== 프롬프트(예시 톤 그대로) =====
    private String buildAllocationPrompt(String summary) {
        return """
역할: 당신은 초보 투자자에게 자산 분배 비율을 쉽고 차분하게 설명하는 해설가다.
스타일: 쉬운 한국어, 짧은 문장, 함축적인 문장, 포트폴리오의 성향 및 위험도 평가.

데이터:
%s

예시:  "
총 자산 5000만원, 월 저축 70만원으로 5년 뒤 10%% 수익을 목표하시는군요. 투자 성향은 안정형, 투자 목표는 저축입니다. 이 목표에 맞춰 자산 분배 비율을 설명해 드리겠습니다.
총 자산 5000만원 중 현금은 10%%인 500만원을 추천합니다. 안정형 투자자에게는 갑작스러운 지출에 대비할 수 있는 여유 자금이 중요하기 때문입니다.
예금은 38%%인 1900만원을 배정했습니다. 목표 금액 달성을 위해 원금 손실 위험 없이 안정적인 이자 수익을 기대할 수 있도록 구성했습니다.
적금은 16%%인 800만원을 추천합니다. 월 저축 금액이 있으시므로, 꾸준히 목표 자산을 모아가는 데 적합합니다.
마지막으로 투자는 36%%인 1800만원을 제안합니다. 안정형 투자 성향과 저축 목표에 맞춰, 너무 공격적이지 않으면서도 예금과 적금만으로는 부족할 수 있는 수익을 보완하도록 했습니다.
종합적으로, 안정적인 성향과 저축 목표에 맞추어 현금·예금·적금 비중을 높여 안정성을 확보하고, 투자를 통해 추가 수익을 기대할 수 있도록 구성했습니다.
요청:  "예시처럼 자산 분배 비율의 이유 설명. 개인 정보(소득/저축/목표/성향/비상금 등)와 추천 비율 간의 **관계**에 근거해 설명. 인사 금지. 사용자의 이름/자기소개 금지. **비율 전체에 대한 평가만** 작성. 데이터 재나열 금지."
""".formatted(summary);
    }


    // ----- Gemini 호출부 -----
    private String generateReasonTextWithGemini(RecommendationResponseDTO.RecommendApiResult rec) {
        String summary = buildSummary(rec);         // 예: 샘플 요약 문자열
        String prompt  = buildPrompt(summary);      // 지시문 + summary

        var req = GeminiRequest.builder()
                .contents(java.util.List.of(
                        GeminiRequest.Content.builder()
                                .parts(java.util.List.of(
                                        GeminiRequest.Part.builder()
                                                .text(prompt)
                                                .build()
                                )).build()
                ))
                .build();

        try {
            var resp = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/" + geminiModel + ":generateContent")
                            .build())
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                            .flatMap(msg -> Mono.error(new IllegalStateException(
                                    "[Gemini] HTTP " + r.statusCode().value() + " - " + msg))))
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(120000))
                    .block();

            if (resp == null || resp.getCandidates() == null || resp.getCandidates().isEmpty()) {
                return null;
            }
            var cand = resp.getCandidates().get(0);
            if (cand.getContent() == null || cand.getContent().getParts() == null || cand.getContent().getParts().isEmpty()) {
                return null;
            }
            return cand.getContent().getParts().get(0).getText();
        } catch (Exception e) {
            // 필요 시 로깅
            return null;
        }
    }

    // ----- 요약 생성: 외부 응답 → 사람이 읽기 쉬운 summary 텍스트 -----
    private String buildSummary(RecommendationResponseDTO.RecommendApiResult r) {
        java.text.DecimalFormat pct2 = new java.text.DecimalFormat("0.00");

        String annualRet = pct2.format(r.getAnnual_return() * 100.0) + "%";
        String annualVol = pct2.format(r.getAnnual_vol() * 100.0) + "%";
        String sharpe    = pct2.format(r.getSharpe());
        String mdd       = pct2.format(r.getMax_drawdown() * 100.0) + "%";

        // weights 상위 3
        var top = r.getWeights() == null ? java.util.List.<java.util.Map.Entry<String, Double>>of()
                : r.getWeights().entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("- 예상 연수익률: ").append(annualRet).append("\n")
                .append("- 예상 연변동성: ").append(annualVol).append("\n")
                .append("- 샤프지수(무위험 0.00%): ").append(sharpe).append("\n")
                .append("- 과거 구간 기준 최대낙폭: ").append(mdd).append("\n")
                .append("- 비중 상위:\n");

        for (var e : top) {
            sb.append("  · ").append(e.getKey()).append(": ")
                    .append(pct2.format(e.getValue() * 100.0)).append("%\n");
        }
        return sb.toString();
    }

    private String buildPrompt(String summary) {
        return """
역할: 당신은 초보 투자자에게 포트폴리오 지표를 쉽고 차분하게 설명하는 해설가다.
스타일: 쉬운 한국어, 짧은 문장, 함축적인 문장, 포트폴리오의 성향 및 위험도 평가.

사전지식 :
- QQQM : Invesco NASDAQ 100 ETF
- 277630.KS : TIGER KOSPI ETF
- 272910.KS : Kim Korea index Active Korea Treasury and agency Bonds Etf
- SPY : SPDR S&P 500 Trust ETF
- IMTB : iShares Core 5 10 Year USD Bond ETF

데이터:
%s

예시:  "이 포트폴리오는 고수익을 추구하면서도 위험을 관리할 수 있도록 구성되었습니다. 특히, 기술주 중심의 미국 시장과 배당 성장형 국내 시장을 혼합하여 위험을 분산하고, 수익률을 끌어올리는 전략을 사용했습니다."
요청:  "예시처럼 해당 데이터 포트폴리오의 성향 및 위험도 분석 및 평가. 제공한 데이터를 다시 나열하지 금지. 자기소개 및 인사말 금지. 평가에 대한 결론만 작성. 오직 포트폴리오에 대한 평가만 작성. 종목에 대한 간단한 설명(Ticker 제외)"
""".formatted(summary);
    }

    private enum Horizon { SHORT, MEDIUM, LONG }

    private static Horizon horizonOf(InvestmentPeriod p) {
        if (p == null) return Horizon.SHORT;
        return switch (p) {
            case UNDER_6_MONTHS, UNDER_1_YEAR, UNDER_2_YEARS, UNDER_3_YEARS -> Horizon.SHORT;
            case UNDER_5_YEARS -> Horizon.MEDIUM;
            case UNDER_10_YEARS, OVER_10_YEARS -> Horizon.LONG;
        };
    }
    private String formatMoney(BigDecimal v) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        return nf.format(nvl(v)) + "원";
    }

    private String toKoPropensity(Propensity p) {
        if (p == null) return "미입력";
        return switch (p) {
            case STABLE -> "안정형";
            case SAFETY -> "보수형";
            case ACTIVE -> "위험중립형";
            case AGGRESSIVE -> "적극형";
            case AGGRESSIVE_INVESTMENT -> "공격투자형";
        };
    }
    private String toKoPeriod(InvestmentPeriod p) {
        if (p == null) return "미입력";
        return switch (p) {
            case UNDER_6_MONTHS -> "6개월 이내";
            case UNDER_1_YEAR   -> "1년 이내";
            case UNDER_2_YEARS  -> "2년 이내";
            case UNDER_3_YEARS  -> "3년 이내";
            case UNDER_5_YEARS  -> "5년 이내";
            case UNDER_10_YEARS -> "10년 이내";
            case OVER_10_YEARS  -> "10년 이상";
        };
    }
    private String toKoPurpose(InvestmentPurpose p) {
        if (p == null) return "미입력";
        return switch (p) {
            case SAVINGS -> "저축";
            case TRAVEL -> "여행";
            case SELF_DEVELOPMENT -> "자기계발";
            case CAR_PURCHASE -> "차량 구입";
            case MARRIAGE -> "결혼";
            case HOME_OWNERSHIP -> "주택 구입";
            default -> "기타";
        };
    }
    private String toKoIncomeRange(IncomeRange r) {
        if (r == null) return "미입력";
        return switch (r) {
            case BELOW_100 -> "100만원 미만";
            case BETWEEN_100_200 -> "100~200만원";
            case BETWEEN_200_300 -> "200~300만원";
            case BETWEEN_300_500 -> "300~500만원";
            case ABOVE_500 -> "500만원 이상";
        };
    }
    private String toKoSavingRange(SavingRange r) {
        if (r == null) return "미입력";

        return switch (r) {

            case BELOW_10, BETWEEN_10_50 -> "50만원 미만";
            case BETWEEN_50_100 -> "50~100만원";
            case BETWEEN_100_200 -> "100~200만원";
            case ABOVE_200 -> "200만원 이상";
        };
    }
    private String toKoProfitRange(ProfitRange r) {
        if (r == null) return "미입력";
        // 점수 로직 기준(대략): ≤10% / 10~15% / 15%+
        return switch (r) {
            case BELOW_500, BETWEEN_500_1000 -> "10% 이하";
            case BETWEEN_1000_3000 -> "10~15%";
            case BETWEEN_3000_5000, BETWEEN_5000_10000, ABOVE_10000 -> "15% 이상";
        };
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
