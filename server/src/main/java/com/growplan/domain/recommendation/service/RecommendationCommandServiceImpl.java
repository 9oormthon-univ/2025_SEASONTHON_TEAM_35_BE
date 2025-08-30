package com.growplan.domain.recommendation.service;

import com.growplan.domain.asset.converter.AssetConverter;
import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.converter.RecommendationConverter;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
import com.growplan.domain.recommendation.entity.InvestmentDesign;
import com.growplan.domain.recommendation.repository.InvestmentDesignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationCommandServiceImpl implements RecommendationCommandService {

    private final InvestmentDesignRepository investmentDesignRepository;

    @Override
    public void designInvestmentPlan(Member member, RecommendationRequestDTO.InvestmentDesignRequest request) {

        InvestmentDesign design = investmentDesignRepository.findByMember(member)
                .orElseGet(() -> RecommendationConverter.toCreateDesign(member, request));

        investmentDesignRepository.save(design);
    }
}
