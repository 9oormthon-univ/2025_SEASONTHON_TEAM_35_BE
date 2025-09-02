//package com.growplan.domain.recommendation.dto;
//
//
//import lombok.Data;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.time.ZonedDateTime;
//
//@Data
//public class ETFQuoteRequestDTO {
//
//    private String symbol;          // "QQQM", "277630.KS" (요청 원본 심볼)
//    private String normalized;      // TwelveData용 정규화 심볼 ("QQQM", "277630:KRX")
//    private String name;            // "Invesco NASDAQ 100 ETF", "TIGER 코스피" 등 (옵션)
//    private String currency;        // "USD", "KRW" 등
//    private double priceClose;      // 최신 종가
//    private Double prevClose;       // 전일 종가(없으면 null)
//    private Double change;          // priceClose - prevClose
//    private Double changePercent;   // (change / prevClose) * 100
//    private String exchange;        // "NASDAQ", "XKRX" 등
//    private ZonedDateTime asOf;     // EOD 시각(거래소 TZ -> 시스템 TZ 변환)
//
//    public static ETFQuoteRequestDTO of(
//            String symbol, String normalized, String name, String currency,
//            double priceClose, Double prevClose, String exchange, ZonedDateTime asOf) {
//        ETFQuoteRequestDTO d = new ETFQuoteRequestDTO();
//        d.symbol = symbol; d.normalized = normalized; d.name = name;
//        d.currency = currency; d.priceClose = priceClose; d.prevClose = prevClose;
//        d.exchange = exchange; d.asOf = asOf;
//        if (prevClose != null && prevClose != 0.0) {
//            d.change = priceClose - prevClose;
//            d.changePercent = (d.change / prevClose) * 100.0;
//        }
//        return d;
//    }
//}
