package com.growplan.domain.recommendation.service;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;

public interface RecommendationCommandService {

    void designInvestmentPlan(Member member, RecommendationRequestDTO.InvestmentDesignRequest request);
    RecommendationResponseDTO.AIPortfolioResponse getAiRecommendedPortfolio(Member member);
    RecommendationResponseDTO.RecommendApiResult getExternalInvestment(Member member);
}
