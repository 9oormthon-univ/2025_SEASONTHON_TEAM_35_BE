package com.growplan.domain.goal.entity;

import com.growplan.domain.member.entity.Member;
import com.growplan.global.common.BaseEntity;
import com.growplan.global.common.enums.*;
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
public class Goal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long goalId;

    @Enumerated(EnumType.STRING)
    private SavingRange savingRange; // 월 저축 가능 금액

    @Enumerated(EnumType.STRING)
    private IncomeRange incomeRange; // 월 소득 범위

    @Enumerated(EnumType.STRING)
    private ProfitRange profitRange; // 기대 수익률

    @Enumerated(EnumType.STRING)
    private InvestmentPeriod investmentPeriod; // 투자 기간

    @Enumerated(EnumType.STRING)
    private Propensity propensity; // 투자 성향

    @Enumerated(EnumType.STRING)
    private InvestmentPurpose investmentPurpose; // 투자 목적

    Boolean emergencyFund; // 비상금 마련 여부

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToOne(mappedBy = "goal", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GoalAnalysis goalAnalysis;
}
