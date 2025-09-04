package com.growplan.domain.goal.converter;

import com.growplan.domain.goal.dto.GoalResponseDTO;
import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.enums.InvestmentPurpose;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GoalConverter {

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final int MONEY_SCALE = 0; // 만원 단위

    private GoalConverter() {}

    public static GoalResponseDTO.GoalAnalysisResponseDTO toAnalysisResponse(
            BigDecimal totalAmount,
            BigDecimal targetAmount,
            int emergencyFund,
            InvestmentPurpose purpose,
            int neededMonthlySaving,
            String feasibilityText
    ) {
        BigDecimal total = nvl(totalAmount).setScale(MONEY_SCALE, RM);
        BigDecimal target = nvl(targetAmount).setScale(MONEY_SCALE, RM);
        BigDecimal emergency = BigDecimal.valueOf(emergencyFund).setScale(MONEY_SCALE, RM);

        StringBuilder sb = new StringBuilder();
        sb.append("현재 자산은 ").append(total.toPlainString()).append("원이며, ")
                .append("매달 약 ").append(neededMonthlySaving).append("원 저축이 필요합니다.\n")
                .append(feasibilityText).append("\n")
                .append("예상치 못한 지출 대비 비상 자금은 최소 ")
                .append(emergency.toPlainString()).append("원 이상 유지하시길 권장드립니다.");

        return GoalResponseDTO.GoalAnalysisResponseDTO.builder()
                .totalAmount(total)
                .targetAmount(target)
                .emergencyFundAmount(emergency)
                .investmentPurpose(purpose)
                .analysisText(sb.toString())
                .build();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

}
