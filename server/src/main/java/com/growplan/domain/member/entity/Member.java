package com.growplan.domain.member.entity;

import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.goal.entity.Goal;
import com.growplan.domain.recommendation.entity.Recommendation;
import com.growplan.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    private String name;

    private String email;

    @Column(name = "provider", nullable = true, length = 10)
    private String provider;

    @Column(name = "provider_id", nullable = true, length = 60)
    private String providerId;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AssetPortfolio> assetPortfolio = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Recommendation> recommendation = new ArrayList<>();

    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Goal goal;
}
