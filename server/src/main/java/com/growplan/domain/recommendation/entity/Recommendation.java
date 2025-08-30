package com.growplan.domain.recommendation.entity;

import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.BaseEntity;
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
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationId;

    @Lob
    private String reasonText; // 자산 비율 추천 이유

    @Lob
    private String analysisText; // 투자 분석 추천 이유

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private InvestmentDesign design;
}
