package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SavingRange { // 월 저축 가능 금액

    BELOW_10,       // 10만원 이하
    BETWEEN_10_50,    // 10-50만원
    BETWEEN_50_100,   // 50-100만원
    BETWEEN_100_200,  // 100-200만원
    ABOVE_200       // 200만원 이상
}
