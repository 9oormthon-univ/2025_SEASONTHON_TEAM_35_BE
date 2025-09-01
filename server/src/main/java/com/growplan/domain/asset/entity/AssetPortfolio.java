package com.growplan.domain.asset.entity;

import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.BaseEntity;
import com.growplan.global.common.enums.AssetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AssetPortfolio extends BaseEntity { // 자산 정보

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetPortfolioId;

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO; // 자산 총액

    @Builder.Default
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal totalInvestedAmount = BigDecimal.ZERO; // 투자 총액

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder.Default
    @OneToMany(mappedBy = "assetPortfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AssetItem> items = new ArrayList<>();

    /**
     *
     * 자산 업데이트 로직
     */

    private static final int PCT_SCALE = 2; // 소수점 둘째 자리까지
    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final Set<AssetType> INVESTMENT_TYPES =
            EnumSet.of(AssetType.STOCK, AssetType.BITCOIN, AssetType.BOND, AssetType.ETF);


    public void recalculateTotalsAndPercentages() {

        BigDecimal newTotalAmount = items.stream()
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 총 자산 계산

        BigDecimal newTotalInvestedAmount = items.stream()
                .filter(item -> INVESTMENT_TYPES.contains(item.getAssetType()))
                .map(item -> item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 총 투자액 계산

        // 포트폴리오 총액 업데이트
        this.totalAmount = newTotalAmount;
        this.totalInvestedAmount = newTotalInvestedAmount;

        // 각 자산 아이템의 비율 계산 및 업데이트
        for (AssetItem item : this.items) {
            BigDecimal percentage = calculatePercentage(item, newTotalAmount, newTotalInvestedAmount);
            item.setPercentage(percentage);
        }
    }

    private BigDecimal calculatePercentage(AssetItem item, BigDecimal total, BigDecimal invested) {

        if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PCT_SCALE, RM);
        }

        BigDecimal base = INVESTMENT_TYPES.contains(item.getAssetType()) ? invested : total;

        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PCT_SCALE, RM);
        }

        return item.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(base, PCT_SCALE, RM);
    }

    public void addItem(AssetItem item) {
        item.setAssetPortfolio(this);
        this.items.add(item);
    }

    public void replaceItems(List<AssetItem> newItems) {
        // 기존 연관 끊기
        for (AssetItem old : new ArrayList<>(this.items)) {
            old.setAssetPortfolio(null);
        }
        this.items.clear();
        // 새 아이템 연결
        for (AssetItem ni : newItems) addItem(ni);
    }

    public void updateTotals(BigDecimal totalAmount, BigDecimal totalInvestedAmount) { // 자산 총액 저장
        this.totalAmount = totalAmount;
        this.totalInvestedAmount = totalInvestedAmount;
    }
}
