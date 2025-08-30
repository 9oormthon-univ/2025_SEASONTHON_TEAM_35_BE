package com.growplan.domain.asset.service;

import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.member.entity.Member;

public interface AssetQueryService {

    AssetResponseDTO.RegisterAssetResponseDTO getAssetSummary(Member member);
}
