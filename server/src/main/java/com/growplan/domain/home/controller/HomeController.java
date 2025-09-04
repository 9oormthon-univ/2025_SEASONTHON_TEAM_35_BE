package com.growplan.domain.home.controller;

import com.growplan.domain.home.dto.HomeResponseDTO;
import com.growplan.domain.home.service.HomeService;
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
@RequestMapping("/api/v0/home")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/")
    @Operation(summary = "홈 화면 API", description = "회원의 홈 화면 정보를 조회합니다. 예산 자산 수익률은 아직 구현이 안 됐습니다.")
    public ApiResponse<HomeResponseDTO.OverviewDTO> getHomeOverview(@CurrentMember Member member) {

        HomeResponseDTO.OverviewDTO response = homeService.getOverview(member);
        return ApiResponse.onSuccess(response);
    }
}
