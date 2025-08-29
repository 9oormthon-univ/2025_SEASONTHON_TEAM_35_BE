package com.growplan.domain.asset.entity;

import com.growplan.global.common.BaseEntity;
import com.growplan.global.common.enums.AssetType;
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
public class AssetItem extends BaseEntity { // 자산 종류

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetItemId;

    @Enumerated(EnumType.STRING)
    private AssetType assetType; // 자산 종류

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private Double percentage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assetPortfolio_id", nullable = false)
    private AssetPortfolio assetPortfolio;
}
