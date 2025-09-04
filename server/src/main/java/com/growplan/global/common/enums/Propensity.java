package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Propensity { // 투자 성향

    STABLE, // 안정형
    SAFETY, // 안전추구형
    ACTIVE, // 위험중립형
    AGGRESSIVE, // 적극투자형
    AGGRESSIVE_INVESTMENT, // 공격투자형
}
