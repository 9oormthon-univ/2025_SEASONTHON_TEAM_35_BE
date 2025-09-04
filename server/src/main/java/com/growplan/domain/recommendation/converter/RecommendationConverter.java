package com.growplan.domain.recommendation.converter;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.dto.PyRecDtos;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;
import com.growplan.domain.recommendation.entity.InvestmentDesign;
import com.growplan.global.common.enums.IncomeRange;
import com.growplan.global.common.enums.InvestmentPeriod;
import com.growplan.global.common.enums.Propensity;

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
            IncomeRange incomeRange,
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
                .recommendationMessage(null)    // 추후 AI 추천 메시지 추가 예정
                .totalAmount(total)
                .incomeRange(incomeRange)
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

    public static RecommendationResponseDTO.EFResult toEfResult(PyRecDtos.RecResponse py) {
        if (py == null || py.metrics == null) return null;

        var weightsPct = new java.util.TreeMap<String, java.math.BigDecimal>();
        if (py.metrics.weights != null) {
            py.metrics.weights.forEach((k,v) ->
                    weightsPct.put(k, java.math.BigDecimal.valueOf(v * 100).setScale(PCT_SCALE, RM))
            );
        }

        return RecommendationResponseDTO.EFResult.builder()
                .weightsPct(weightsPct)
                .annualReturnPct(java.math.BigDecimal.valueOf(py.metrics.annual_return * 100).setScale(PCT_SCALE, RM))
                .annualVolPct(java.math.BigDecimal.valueOf(py.metrics.annual_vol * 100).setScale(PCT_SCALE, RM))
                .sharpe(py.metrics.sharpe == null ? null :
                        java.math.BigDecimal.valueOf(py.metrics.sharpe).setScale(2, RM))
                .maxDrawdownPct(java.math.BigDecimal.valueOf(py.metrics.max_drawdown * 100).setScale(PCT_SCALE, RM))
                .build();
    }

    public static RecommendationResponseDTO.EtfSnapshot toEtfSnapshot(com.fasterxml.jackson.databind.JsonNode json) {
        if (json == null) return null;

        java.util.function.Function<com.fasterxml.jackson.databind.JsonNode, java.util.Map<String, java.math.BigDecimal>> toBigMap =
                node -> {
                    if (node == null || node.isNull()) return null;
                    java.util.Map<String, java.math.BigDecimal> m = new java.util.TreeMap<>();
                    node.fields().forEachRemaining(e ->
                            m.put(e.getKey(), e.getValue().isNumber()
                                    ? e.getValue().decimalValue()
                                    : new java.math.BigDecimal(e.getValue().asText("0"))));
                    return m;
                };

        var ef = json.get("metrics") == null ? null :
                RecommendationResponseDTO.EFResult.builder()
                        .weightsPct(toBigMap.apply(json.get("metrics").get("weights")).entrySet().stream()
                                .collect(java.util.stream.Collectors.toMap(
                                        java.util.Map.Entry::getKey,
                                        v -> v.getValue().multiply(java.math.BigDecimal.valueOf(100)).setScale(PCT_SCALE, RM),
                                        (a,b)->a, java.util.TreeMap::new)))
                        .annualReturnPct(java.math.BigDecimal.valueOf(json.get("metrics").get("annual_return").asDouble()*100).setScale(PCT_SCALE, RM))
                        .annualVolPct(java.math.BigDecimal.valueOf(json.get("metrics").get("annual_vol").asDouble()*100).setScale(PCT_SCALE, RM))
                        .sharpe(json.get("metrics").get("sharpe").isNull() ? null :
                                java.math.BigDecimal.valueOf(json.get("metrics").get("sharpe").asDouble()).setScale(2, RM))
                        .maxDrawdownPct(java.math.BigDecimal.valueOf(json.get("metrics").get("max_drawdown").asDouble()*100).setScale(PCT_SCALE, RM))
                        .build();

        return RecommendationResponseDTO.EtfSnapshot.builder()
                .lastPrices(toBigMap.apply(json.get("last_prices")))
                .dayChangePct(toBigMap.apply(json.get("day_change_pct")))
                .asOf(json.get("as_of") == null ? null : json.get("as_of").asText())
                .efResult(ef)
                .build();
    }
}
