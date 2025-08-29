package com.growplan.domain.asset.entity;

import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AssetPortfolio extends BaseEntity { // 자산 정보

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetPortfolioId;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount; // 자산 총액

    @Column(precision = 19, scale = 4)
    private BigDecimal totalInvestedAmount; // 투자 총액

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(mappedBy = "assetPortfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AssetItem assetItem;

}
