package com.growplan.domain.asset.controller;

import com.growplan.domain.asset.dto.AssetRequestDTO;
import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.asset.service.AssetCommandService;
import com.growplan.domain.asset.service.AssetQueryService;
import com.growplan.domain.member.entity.Member;
import com.growplan.global.annotation.CurrentMember;
import com.growplan.global.error.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/asset")
public class AssetController {

    private final AssetCommandService assetCommandService;
    private final AssetQueryService assetQueryService;

    @PostMapping("/register")
    @Operation(summary = "자산 등록 API", description = "회원의 자산을 등록합니다")
    public ApiResponse<Void> registerAsset(@CurrentMember Member member,
                                           @RequestBody @Valid AssetRequestDTO.RegisterAssetRequestDTO request) {
        assetCommandService.registerAsset(member, request);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/summary")
    @Operation(summary = "자산 요약 조회 API", description = "회원의 자산 요약 정보를 조회합니다.")
    public ApiResponse<AssetResponseDTO.RegisterAssetResponseDTO> getAssetSummary(@CurrentMember Member member) {
        AssetResponseDTO.RegisterAssetResponseDTO response = assetQueryService.getAssetSummary(member);
        return ApiResponse.onSuccess(response);
    }
}
