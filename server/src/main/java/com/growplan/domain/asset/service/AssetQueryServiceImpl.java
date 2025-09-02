package com.growplan.domain.asset.service;

import com.growplan.domain.asset.converter.AssetConverter;
import com.growplan.domain.asset.dto.AssetRequestDTO;
import com.growplan.domain.asset.dto.AssetResponseDTO;
import com.growplan.domain.asset.entity.AssetItem;
import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.asset.repository.AssetPortfolioRepository;
import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.enums.AssetType;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.handler.AssetException;
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
@RequiredArgsConstructor
public class AssetQueryServiceImpl implements AssetQueryService {

    private final AssetPortfolioRepository assetPortfolioRepository;
    private static final Set<AssetType> INVESTMENT_TYPES =
            EnumSet.of(AssetType.STOCK, AssetType.BITCOIN, AssetType.BOND, AssetType.ETF); // 투자 자산

    @Override
    @Transactional(readOnly = true)
    public AssetResponseDTO.RegisterAssetResponseDTO getAssetSummary(Member member) {

        AssetPortfolio portfolio = assetPortfolioRepository.findByMember(member)
                .orElseGet(() -> AssetConverter.toAssetPortfolio(member));
        List<AssetItem> items = portfolio.getItems(); // LAZY, 트랜잭션 내에서 접근

        // 총액
        BigDecimal totalAmount = sumAll(items);
        // 현금
        BigDecimal cash = sumByType(items, AssetType.CASH);
        // 투자
        BigDecimal totalInvestedAmount = sumByInvestment(items, INVESTMENT_TYPES);
        // 기타 = 총액 - (현금+투자), 음수 방지
        BigDecimal other = safe(totalAmount.subtract(cash).subtract(totalInvestedAmount));

        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new AssetException(ErrorStatus.ASSET_NOT_FOUND);
        }

        portfolio.updateTotals(totalAmount, totalInvestedAmount);

        BigDecimal stock = sumByType(items, AssetType.STOCK);
        BigDecimal bitcoin = sumByType(items, AssetType.BITCOIN);
        BigDecimal bond = sumByType(items, AssetType.BOND);
        BigDecimal etf = sumByType(items, AssetType.ETF);

        return toRegisterAssetResponse(totalAmount, cash, totalInvestedAmount, other, stock, bitcoin, bond, etf);
    }

    @Override
    public void updateCash(Member member, AssetRequestDTO.UpdateCashRequest request) { // 현금 수정

        AssetPortfolio portfolio = getOrCreatePortfolio(member);

        upsertItem(portfolio, AssetType.CASH, nz(request.getAmount())); // 현금 수정
        portfolio.recalculateTotalsAndPercentages(); // 모든 총액 및 비율 재계산

        assetPortfolioRepository.save(portfolio);
    }

    @Override
    public AssetResponseDTO.updateInvestmentResponseDTO updateInvestment(Member member, AssetRequestDTO.UpdateInvestmentsRequest request) { // 투자 수정

        AssetPortfolio portfolio = getOrCreatePortfolio(member);

        if (request.getStockAmount() != null) upsertItem(portfolio, AssetType.STOCK, nz(request.getStockAmount()));
        if (request.getBitcoinAmount() != null) upsertItem(portfolio, AssetType.BITCOIN, nz(request.getBitcoinAmount()));
        if (request.getBondAmount() != null) upsertItem(portfolio, AssetType.BOND, nz(request.getBondAmount()));
        if (request.getEtfAmount() != null) upsertItem(portfolio, AssetType.ETF, nz(request.getEtfAmount()));

        portfolio.recalculateTotalsAndPercentages(); // 모든 총액 및 비율 재계산
        assetPortfolioRepository.save(portfolio);

        return AssetResponseDTO.updateInvestmentResponseDTO.builder()
                .stockAmount(request.getStockAmount())
                .bitcoinAmount(request.getBitcoinAmount())
                .bondAmount(request.getBondAmount())
                .etfAmount(request.getEtfAmount())
                .build();
    }

    @Override
    public void updateOthers(Member member, AssetRequestDTO.UpdateOthersRequest request) { // 기타 자산 수정

        AssetPortfolio portfolio = getOrCreatePortfolio(member);
        if (request.getItems() == null) return;

        for (AssetRequestDTO.ItemRequest it : request.getItems()) {

            AssetType t = it.getAssetType();
            if (t == null) continue;
            if (t == AssetType.CASH || INVESTMENT_TYPES.contains(t)) {
                throw new AssetException(ErrorStatus.ASSET_ITEM_INVALID_REQUEST);
            }
            upsertItem(portfolio, t, nz(it.getAmount()));
        }

        portfolio.recalculateTotalsAndPercentages(); // 모든 총액 및 비율 재계산
        assetPortfolioRepository.save(portfolio);
    }

    private AssetPortfolio getOrCreatePortfolio(Member member) {

        return assetPortfolioRepository.findByMember(member)
                .orElseGet(() -> AssetConverter.toAssetPortfolio(member));
    }

    private void upsertItem(AssetPortfolio portfolio, AssetType type, BigDecimal amount) {

        AssetItem found = portfolio.getItems().stream()
                .filter(i -> i.getAssetType() == type)
                .findFirst()
                .orElse(null);

        if (found == null) {
            AssetItem n = AssetItem.builder()
                    .assetType(type)
                    .amount(amount)
                    .build();
            portfolio.addItem(n);
        } else {
            found.setAmount(amount);
        }
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
