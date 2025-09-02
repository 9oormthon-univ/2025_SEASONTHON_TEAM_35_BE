package com.growplan.domain.recommendation.controller;

import com.growplan.domain.member.entity.Member;
//import com.growplan.domain.recommendation.dto.ETFQuoteRequestDTO;
import com.growplan.domain.recommendation.dto.RecommendationRequestDTO;
//import com.growplan.domain.recommendation.service.ETFQuoteService;
import com.growplan.domain.recommendation.dto.RecommendationResponseDTO;
import com.growplan.domain.recommendation.etf.ETFQuoteStore;
import com.growplan.domain.recommendation.service.ETFQuoteQueryService;
import com.growplan.domain.recommendation.service.RecommendationCommandService;
import com.growplan.global.annotation.CurrentMember;
import com.growplan.global.error.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/recommendation")
public class RecommendationController { // 자산 설계 및 분석

    private final RecommendationCommandService recommendationCommandService;
    private final ETFQuoteQueryService etfQuoteQueryService;

    @PostMapping("/design")
    @Operation(summary = "투자 설계 API", description = "회원의 투자 설계를 진행합니다. 자세한 enum 설명은 노션에 정리하겠습니다.")
    public ApiResponse<Void> designInvestment(@CurrentMember Member member,
                                              @RequestBody @Valid RecommendationRequestDTO.InvestmentDesignRequest request) {
        recommendationCommandService.designInvestmentPlan(member, request);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/investment")
    @Operation(summary = "투자 분석 API", description = "회원의 투자 분석 결과를 조회합니다.")
    public ApiResponse<RecommendationResponseDTO.InvestmentSummaryResponse> getInvestmentSummary() {
        var list = etfQuoteQueryService.currentQuotesEnsureAll(); // ★ 항상 5개 보장
        var dto  = RecommendationResponseDTO.InvestmentSummaryResponse.builder()
                .etfList(list)
                .build();
        return ApiResponse.onSuccess(dto);
    }
}
