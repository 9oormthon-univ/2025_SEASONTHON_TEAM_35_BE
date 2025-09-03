package com.growplan.domain.recommendation.controller;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;
import com.growplan.domain.recommendation.service.RecommendationCommandService;
import com.growplan.global.annotation.CurrentMember;
import com.growplan.global.error.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/recommendation")
public class RecommendationController { // 자산 설계 및 분석

    private final RecommendationCommandService recommendationCommandService;

    @PostMapping("/design")
    @Operation(summary = "투자 설계 API", description = "회원의 투자 설계를 진행합니다. 자세한 enum 설명은 노션에 정리하겠습니다.")
    public ApiResponse<Void> designInvestment(@CurrentMember Member member,
                                              @RequestBody @Valid RecommendationRequestDTO.InvestmentDesignRequest request) {
        recommendationCommandService.designInvestmentPlan(member, request);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/portfolio")
    @Operation(summary = "AI 추천 자산 비율 API", description = "회원의 포트폴리오를 추천합니다.<br/> AI 자산 의견은 아직입니다! 완성될 시 말씀드리겠습니다.")
    public ApiResponse<RecommendationResponseDTO.AIPortfolioResponse> getAiRecommendedPortfolio(@CurrentMember Member member) {

        RecommendationResponseDTO.AIPortfolioResponse response = recommendationCommandService.getAiRecommendedPortfolio(member);
        return ApiResponse.onSuccess(response);
    }
}
