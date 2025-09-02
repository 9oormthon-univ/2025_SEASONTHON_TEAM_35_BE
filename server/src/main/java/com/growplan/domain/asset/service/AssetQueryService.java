package com.growplan.domain.asset.service;

import com.growplan.domain.asset.dto.AssetRequestDTO;
import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.member.entity.Member;

public interface AssetQueryService {

    AssetResponseDTO.RegisterAssetResponseDTO getAssetSummary(Member member);
    void updateCash(Member member, AssetRequestDTO.UpdateCashRequest request);
    void updateDeposit(Member member, AssetRequestDTO.UpdateDepositRequest request);
    void updateSavings(Member member, AssetRequestDTO.UpdateSavingsRequest request);
    AssetResponseDTO.updateInvestmentResponseDTO updateInvestment(Member member, AssetRequestDTO.UpdateInvestmentsRequest request);
    void updateOthers(Member member, AssetRequestDTO.UpdateOthersRequest request);
}
