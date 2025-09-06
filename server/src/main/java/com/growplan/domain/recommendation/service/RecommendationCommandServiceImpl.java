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

//        if (design == null) {
//            return toResponse(
//                    totalAmount,
//                    0.0, 0.0, 0.0, 0.0,
//                    "투자 설계가 등록되어 있지 않습니다.",
//                    null, // IncomeRange
//                    null, // InvestmentPeriod
//                    null  // Propensity
//            );
//        }

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

        String recommendationMessage = buildRecommendationMessage(design);

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
    public RecommendationResponseDTO.RecommendApiResult getExternalInvestment(Member member) {

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
                    .timeout(Duration.ofSeconds(12))
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

    // ----- 프롬프트 생성: (PM님이 준 파이썬 예시를 Java 문자열로) -----
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
요청:  "예시처럼 해당 데이터 포트폴리오의 성향 및 위험도 분석 및 평가. 제공한 데이터를 다시 나열하지 금지. 자기소개 및 인사말 금지. 평가에 대한 결론만 작성. 오직 포트폴리오에 대한 평가만 작성"
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
    private static boolean isConservative(Propensity p) {
        return p == Propensity.STABLE || p == Propensity.SAFETY;
    }
    private static boolean isAggressive(Propensity p) {
        return p == Propensity.AGGRESSIVE || p == Propensity.AGGRESSIVE_INVESTMENT;
    }
    private static boolean isBalanced(Propensity p) {
        return p == Propensity.ACTIVE;
    }

    private String buildRecommendationMessage(InvestmentDesign d) {
        Propensity p = d.getPropensity();
        boolean needEF = Boolean.TRUE.equals(d.getEmergencyFund());
        Horizon h = horizonOf(d.getInvestmentPeriod());

        // 1) 안정적 + 비상금 필요 + 단기  (피그마 예시 문구)
        if (isConservative(p) && needEF && h == Horizon.SHORT) {
            return "안정적인 운용을 선호하시기 때문에 예금과 비상금 중심으로 추천드렸습니다. "
                    + "급한 상황에서도 자산을 쉽게 사용할 수 있도록 유동성을 확보했어요. "
                    + "목표 시점이 가까워 위험 자산 비중을 졸이고, 원금 보전에 유리한 구조로 설정했습니다.";
        }

        // 2) 안정적 + 비상금 필요 + 장기
        if (isConservative(p) && needEF && h == Horizon.LONG) {
            return "장기적인 안목으로 안정성을 추구하시는 성향에 맞춰, 비상금은 별도로 안전하게 확보하는 것을 추천합니다. "
                    + "나머지 자금은 국채나 우량 채권 중심으로 꾸준히 운용하며, 정기적인 리밸런싱으로 변동성을 관리해 장기적인 자산 증식을 목표로 합니다.";
        }

        // 3) 공격적 + 비상금 필요 + 장기
        if (isAggressive(p) && needEF && h == Horizon.LONG) {
            return "높은 수익을 추구하시는 성향과 장기 목표를 고려하여 투자 자산 비중을 높게 설정했습니다. "
                    + "다만, 최소한의 안전장치로 3~6개월치 비상금을 확보해 유동성 위험을 줄이는 것이 중요합니다. "
                    + "이후 장기적인 분산투자와 리밸런싱으로 시장 하락 시 손실 위험을 관리하며 복리 효과를 극대화하는 전략을 추천합니다.";
        }

        // 4) 공격적 + 비상금 필요 + 단기
        if (isAggressive(p) && needEF && h == Horizon.SHORT) {
            return "공격적인 운용을 선호하시지만 목표 시점이 가까워 원금 보전을 우선했습니다. "
                    + "갑작스러운 시장 하락에도 원금을 보호할 수 있도록 예금과 단기 채권 위주로 운용하도록 설정하였습니다.";
        }

        // 5) 위험중립 + 비상금 필요 + 단기
        if (isBalanced(p) && needEF && h == Horizon.SHORT) {
            return "단기 목표 달성을 위해 안정성을 높이는 방향으로 추천했습니다. "
                    + "비상금을 통해 유동성을 확보하고, 자산의 대부분은 예금 및 단기 채권에 보관합니다. "
                    + "소량의 우량 주식이나 배당주를 편입하여 안정적인 추가 수익을 추구하는 전략을 고려해볼 수 있습니다.";
        }

        // 6) 위험중립 + 비상금 필요 + 장기
        if (isBalanced(p) && needEF && h == Horizon.LONG) {
            return "위험중립 성향과 장기 목표의 균형을 맞추기 위해 '자산배분' 전략을 추천합니다. "
                    + "주식과 채권의 비중을 균형 있게 배분하여 안정적인 성장을 추구합니다. "
                    + "비상금은 별도로 유지하고, 꾸준한 분할 투자와 정기적인 리밸런싱으로 장기적인 수익성과 안정성을 함께 관리하는 것이 중요합니다.";
        }

        // 7) 안정적 + 비상금 없음 + 단기
        if (isConservative(p) && !needEF && h == Horizon.SHORT) {
            return "안정성을 선호하시지만 비상금이 없어 예상치 못한 지출에 취약할 수 있습니다. "
                    + "투자에 앞서, 최소 3개월치 생활비를 언제든 사용할 수 있는 예금이나 파킹통장으로 먼저 마련하는 것을 강력히 권고합니다. "
                    + "이후 단기 목표에 맞춰 안전자산 위주로 운용하는 것이 바람직합니다.";
        }

        // 8) 안정적 + 비상금 없음 + 장기
        if (isConservative(p) && !needEF && h == Horizon.LONG) {
            return "장기 투자 전에 비상금을 먼저 마련해 예기치 못한 지출에 대비하세요. "
                    + "먼저 3~6개월치 생활비를 확보한 후, 장기적인 관점에서 채권과 예금 중심으로 투자를 시작하고 점진적으로 투자 자산을 늘려가는 전략을 추천합니다.";
        }

        // 9) 공격적 + 비상금 없음 + 장기
        if (isAggressive(p) && !needEF && h == Horizon.LONG) {
            return "공격적 성향이라도 비상금이 없으면 손절·중도해지 리스크가 커집니다. "
                    + "먼저 3~6개월치 비상금을 안전하게 확보하세요. 그 후에 주식형 펀드나 ETF 중심으로 장기 분산 투자하여 기대수익률을 높이는 전략을 실행하는 것이 안전합니다.";
        }

        // 10) 그 외(기본 가이드)
        return "목표 시점과 유동성 필요도를 고려해 비상금을 우선 확보하고, "
                + "남는 자금은 성향에 맞춰 안전자산과 위험자산을 배분했습니다. "
                + "분할투자와 정기 리밸런싱으로 변동성을 관리하는 전략을 권장합니다.";
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
