package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvestmentPeriod { // 투자 기간

    UNDER_6_MONTHS("6개월 이내"),
    UNDER_1_YEAR("1년 이내"),
    UNDER_2_YEARS("2년 이내"),
    UNDER_3_YEARS("3년 이내"),
    UNDER_5_YEARS("5년 이내"),
    UNDER_10_YEARS("10년 이내"),
    OVER_10_YEARS("10년 이상 장기");

    private final String period;
}
