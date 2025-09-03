package com.growplan.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AssetType { // 자산 종류

    CASH("현금"),
    DEPOSIT("예금"),
    SAVINGS("적금"),
    STOCK("주식"),
    BITCOIN("비트코인"),
    BOND("채권"),
    ETF("ETF"),
    OTHER("기타");

    private final String assetType;
}
