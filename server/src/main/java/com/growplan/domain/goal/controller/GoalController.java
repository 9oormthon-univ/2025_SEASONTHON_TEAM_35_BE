package com.growplan.domain.goal.controller;

import com.growplan.domain.goal.dto.GoalResponseDTO;
import com.growplan.domain.goal.service.GoalCommandService;
import com.growplan.domain.member.entity.Member;
import com.growplan.global.annotation.CurrentMember;
import com.growplan.global.error.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/goal")
public class GoalController {

    private final GoalCommandService goalCommandService;

    @GetMapping("/analyze")
    @Operation(summary = "목표 설정 API", description = "회원의 목표를 관리합니다.")
    public ApiResponse<GoalResponseDTO.GoalAnalysisResponseDTO> analyzeGoal(@CurrentMember Member member){

        GoalResponseDTO.GoalAnalysisResponseDTO response = goalCommandService.analyzeGoal(member);
        return ApiResponse.onSuccess(response);
    }
}
