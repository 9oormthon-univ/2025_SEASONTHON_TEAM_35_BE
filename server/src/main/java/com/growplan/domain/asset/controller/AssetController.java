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

//    @PostMapping("/register")
//    @Operation(summary = "자산 등록 API",
//            description = """
//                    회원의 자산을 등록합니다.
//                    <br><br><b>요청 본문(Request Body) 전체 양식 예시:</b>
//                    <pre><code>{
//                      "assetList": [
//                        { "assetType": "CASH", "amount": 10000000 },
//                        { "assetType": "DEPOSIT", "amount": 10000000 },
//                        { "assetType": "SAVINGS", "amount": 10000000 },
//                        { "assetType": "STOCK", "amount": 5000000 },
//                        { "assetType": "BOND", "amount": 2000000 },
//                        { "assetType": "ETF", "amount": 1500000 },
//                        { "assetType": "BITCOIN", "amount": 1000000 },
//                        { "assetType": "OTHER", "amount": 500000 }
//                      ]
//                    }</code></pre>"""
//    )
//    public ApiResponse<Void> registerAsset(@CurrentMember Member member,
//                                           @RequestBody @Valid AssetRequestDTO.RegisterAssetRequestDTO request)
//    {
//        assetCommandService.registerAsset(member, request);
//        return ApiResponse.onSuccess(null);
//    }

    @GetMapping("/summary")
    @Operation(summary = "자산 정보 조회 API", description = "회원의 자산 요약 정보를 조회합니다.")
    public ApiResponse<AssetResponseDTO.RegisterAssetResponseDTO> getAssetSummary(@CurrentMember Member member) {
        AssetResponseDTO.RegisterAssetResponseDTO response = assetQueryService.getAssetSummary(member);
        return ApiResponse.onSuccess(response);
    }

//    @PostMapping("/modify-cash")
//    @Operation(summary = "현금 자산 수정 API", description = "회원의 현금 자산을 수정합니다.")
//    public ApiResponse<Void> updateCash(@CurrentMember Member member,
//                                        @RequestBody @Valid AssetRequestDTO.UpdateCashRequest request) {
//        assetQueryService.updateCash(member, request);
//        return ApiResponse.onSuccess(null);
//    }
//
//    @PostMapping("/modify-deposit")
//    @Operation(summary = "예금 자산 수정 API", description = "회원의 예금 자산을 수정합니다.")
//    public ApiResponse<Void> updateDeposit(@CurrentMember Member member,
//                                        @RequestBody @Valid AssetRequestDTO.UpdateDepositRequest request) {
//        assetQueryService.updateDeposit(member, request);
//        return ApiResponse.onSuccess(null);
//    }
//
//    @PostMapping("/modify-savings")
//    @Operation(summary = "적금 자산 수정 API", description = "회원의 적금 자산을 수정합니다.")
//    public ApiResponse<Void> updateSavings(@CurrentMember Member member,
//                                        @RequestBody @Valid AssetRequestDTO.UpdateSavingsRequest request) {
//        assetQueryService.updateSavings(member, request);
//        return ApiResponse.onSuccess(null);
//    }
//
//    @PostMapping("/modify-investment")
//    @Operation(summary = "투자 자산 수정 API", description = "회원의 투자 자산을 수정합니다. 입력 값을 응답으로 반환합니다.")
//    public ApiResponse<AssetResponseDTO.updateInvestmentResponseDTO> updateInvestment(@CurrentMember Member member,
//                                        @RequestBody @Valid AssetRequestDTO.UpdateInvestmentsRequest request) {
//        AssetResponseDTO.updateInvestmentResponseDTO response = assetQueryService.updateInvestment(member, request);
//        return ApiResponse.onSuccess(response);
//    }
//
//    @PostMapping("/modify-other")
//    @Operation(summary = "기타 자산 수정 API", description = "회원의 기타 자산을 수정합니다.")
//    public ApiResponse<Void> updateOther(@CurrentMember Member member,
//                                        @RequestBody @Valid AssetRequestDTO.UpdateOthersRequest request) {
//        assetQueryService.updateOthers(member, request);
//        return ApiResponse.onSuccess(null);
//    }
}
