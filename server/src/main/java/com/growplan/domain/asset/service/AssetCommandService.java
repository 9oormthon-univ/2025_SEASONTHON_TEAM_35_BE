package com.growplan.domain.asset.service;

import com.growplan.domain.asset.dto.AssetRequestDTO;
import com.growplan.domain.member.entity.Member;

public interface AssetCommandService {

    void registerAsset(Member member, AssetRequestDTO.RegisterAssetRequestDTO request);
}
