package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Propensity { // 투자 성향

    STABLE("안정형"),
    ACTIVE("공격형"),
    COMMON("보통형");

    private final String propensity;
}
