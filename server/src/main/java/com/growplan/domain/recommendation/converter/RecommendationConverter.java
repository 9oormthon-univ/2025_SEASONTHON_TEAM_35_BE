package com.growplan.domain.recommendation.converter;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
import com.growplan.domain.recommendation.entity.InvestmentDesign;

public class RecommendationConverter {

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
}
