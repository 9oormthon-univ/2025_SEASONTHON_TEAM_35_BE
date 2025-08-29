package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProfitRange { // 투자 수익에 대한 목표 금액

    BELOW_500,       // 500만원 이하
    BETWEEN_500_1500, // 500-1500만원
    BETWEEN_1500_3000,// 1500-3000만원
    BETWEEN_3000_5000,// 3000-5000만원
    BETWEEN_5000_10000,// 5000-1억
    ABOVE_10000      // 1억 이상
}
