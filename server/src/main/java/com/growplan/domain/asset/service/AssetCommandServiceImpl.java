package com.growplan.domain.asset.service;

import com.growplan.domain.asset.converter.AssetConverter;
import com.growplan.domain.asset.dto.AssetRequestDTO;
import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.asset.entity.AssetItem;
import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.asset.repository.AssetPortfolioRepository;
import com.growplan.domain.member.entity.Member;
import com.growplan.domain.member.repository.MemberRepository;
import com.growplan.global.common.enums.AssetType;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.handler.AssetException;
import com.growplan.global.error.exception.handler.MemberException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.growplan.domain.asset.converter.AssetConverter.toRegisterAssetResponse;

@Service
@Transactional
@RequiredArgsConstructor
public class AssetCommandServiceImpl implements AssetCommandService {

    private final AssetPortfolioRepository assetPortfolioRepository;
    private static final Set<AssetType> INVESTMENT_TYPES =
            EnumSet.of(AssetType.STOCK, AssetType.BITCOIN, AssetType.BOND, AssetType.ETF); // 투자 자산

    @Override
    public void registerAsset(Member member, AssetRequestDTO.RegisterAssetRequestDTO request) {

        AssetPortfolio portfolio = assetPortfolioRepository.findByMember(member)
                .orElseGet(() -> AssetConverter.toAssetPortfolio(member));

        List<AssetItem> newAssetItems = request.getAssetList().stream()
                .map(AssetConverter::toAssetItem)
                .collect(Collectors.toList());

        portfolio.replaceItems(newAssetItems);

        assetPortfolioRepository.save(portfolio);
    }

}
