package com.growplan.domain.goal.entity;

import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.BaseEntity;
import com.growplan.global.common.enums.InvestmentPurpose;
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
public class Goal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long goalAnalysisId;

    private BigDecimal targetAmount; // 목표 금액

    private BigDecimal emergencyFundAmount; // 비상금 마련 금액

    @Enumerated(EnumType.STRING)
    private InvestmentPurpose investmentPurpose;

    @Lob
    private String analysisText; // 목표 분석

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
