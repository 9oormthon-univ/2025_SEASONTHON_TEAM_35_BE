package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncomeRange {

    BELOW_100,      // 100만원 이하
    BETWEEN_100_200,  // 100-200만원
    BETWEEN_200_300,  // 200-300만원
    BETWEEN_300_500,  // 300-500만원
    ABOVE_500       // 500만원 이상
}
