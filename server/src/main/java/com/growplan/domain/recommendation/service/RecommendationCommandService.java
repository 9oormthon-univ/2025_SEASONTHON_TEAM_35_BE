package com.growplan.domain.recommendation.service;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;

public interface RecommendationCommandService {

    void designInvestmentPlan(Member member, RecommendationRequestDTO.InvestmentDesignRequest request);
}
