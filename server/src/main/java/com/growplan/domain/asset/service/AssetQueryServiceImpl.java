package com.growplan.domain.asset.service;

import com.growplan.domain.asset.converter.AssetConverter;
import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.asset.entity.AssetItem;
import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.asset.repository.AssetPortfolioRepository;
import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.enums.AssetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.growplan.domain.asset.converter.AssetConverter.toRegisterAssetResponse;

@Service
@Transactional
@RequiredArgsConstructor
public class AssetQueryServiceImpl implements AssetQueryService {

    private final AssetPortfolioRepository assetPortfolioRepository;
    private static final Set<AssetType> INVESTMENT_TYPES =
            EnumSet.of(AssetType.STOCK, AssetType.BITCOIN, AssetType.BOND, AssetType.ETF); // 투자 자산

    @Override
    public AssetResponseDTO.RegisterAssetResponseDTO getAssetSummary(Member member) {

        AssetPortfolio portfolio = assetPortfolioRepository.findByMember(member)
                .orElseGet(() -> AssetConverter.toAssetPortfolio(member));
        List<AssetItem> items = portfolio.getItems(); // LAZY, 트랜잭션 내에서 접근

        // 총액
        BigDecimal totalAmount = sumAll(items);
        // 현금
        BigDecimal cash = sumByType(items, AssetType.CASH);
        // 투자(정의된 4종)
        BigDecimal totalInvestedAmount = sumByInvestment(items, INVESTMENT_TYPES);
        // 기타 = 총액 - (현금+투자), 음수 방지
        BigDecimal other = safe(totalAmount.subtract(cash).subtract(totalInvestedAmount));

        portfolio.updateTotals(totalAmount, totalInvestedAmount);

        BigDecimal stock = sumByType(items, AssetType.STOCK);
        BigDecimal bitcoin = sumByType(items, AssetType.BITCOIN);
        BigDecimal bond = sumByType(items, AssetType.BOND);
        BigDecimal etf = sumByType(items, AssetType.ETF);

        return toRegisterAssetResponse(totalAmount, cash, totalInvestedAmount, other, stock, bitcoin, bond, etf);
    }

    private BigDecimal sumAll(List<AssetItem> items) {
        return (items == null ? List.<AssetItem>of() : items).stream()
                .map(i -> nz(i.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByType(List<AssetItem> items, AssetType type) {
        return (items == null ? List.<AssetItem>of() : items).stream()
                .filter(i -> i.getAssetType() == type)
                .map(i -> nz(i.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByInvestment(List<AssetItem> items, Set<AssetType> types) {
        return (items == null ? List.<AssetItem>of() : items).stream()
                .filter(i -> i.getAssetType() != null && types.contains(i.getAssetType()))
                .map(i -> nz(i.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nz(BigDecimal v) { // NPE 방지
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal safe(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO;
        return v.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : v;
    }
}
