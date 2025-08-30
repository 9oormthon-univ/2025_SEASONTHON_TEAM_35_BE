package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvestmentPurpose { // 투자 목적

    SAVINGS,           // 저축
    HOME_OWNERSHIP,    // 내 집 마련
    CAR_PURCHASE,      // 차량 구매
    TRAVEL,            // 여행
    MARRIAGE,          // 결혼
    SELF_DEVELOPMENT,  // 자기계발
    OTHER              // 기타
}
