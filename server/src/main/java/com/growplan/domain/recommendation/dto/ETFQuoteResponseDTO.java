package com.growplan.domain.recommendation.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ETFQuoteResponseDTO {
    // 키: 국내는 6자리(277630), 해외는 EXCD.SYMB(ARCX.SPY)
    private String key;
    // 화면 표기용
    private String symbol;     // 예) "QQQM", "277630.KS"
    private String name;       // 예) "Invesco NASDAQ 100 ETF"
    private String currency;   // "USD" | "KRW"

    // 시세
    private BigDecimal price;      // 현재가
    private BigDecimal change;     // 전일대비 절대값
    private BigDecimal changePct;  // 전일대비 등락률(%)
    private Instant lastUpdated; // 수신 시각

    private String dataSource;   // "LIVE" | "PREV_CLOSE"
    private String session;      // "OPEN" | "CLOSED"
}