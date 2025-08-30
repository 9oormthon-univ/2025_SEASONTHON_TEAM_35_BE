package com.growplan.domain.asset.entity;

import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
