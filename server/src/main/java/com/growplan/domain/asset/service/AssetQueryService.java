package com.growplan.domain.asset.service;

import com.growplan.domain.asset.dto.AssetRequestDTO;
import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.member.entity.Member;

public interface AssetQueryService {

    AssetResponseDTO.RegisterAssetResponseDTO getAssetSummary(Member member);
    void updateInvestment(Member member, AssetRequestDTO.UpdateInvestmentsRequest request);
    void updateCash(Member member, AssetRequestDTO.UpdateCashRequest request);
    void updateOthers(Member member, AssetRequestDTO.UpdateOthersRequest request);
}
