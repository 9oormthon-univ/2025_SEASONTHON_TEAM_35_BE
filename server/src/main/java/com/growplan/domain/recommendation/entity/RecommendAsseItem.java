package com.growplan.domain.recommendation.entity;

import com.growplan.global.common.BaseEntity;
import com.growplan.global.common.enums.AssetType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RecommendAsseItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendAsseItemId;

    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    private Double assetRatio; // 자산 비율
}
