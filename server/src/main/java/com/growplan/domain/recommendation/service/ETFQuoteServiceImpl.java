//package com.growplan.domain.recommendation.service;
//
//import com.growplan.domain.recommendation.dto.ETFQuoteRequestDTO;
//import com.growplan.domain.recommendation.dto.ETQuoteFResponseDTO;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.time.ZonedDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Service
//public class ETFQuoteServiceImpl implements ETFQuoteService {
//
//    private final WebClient client;
//    private final String apiKey;
//
//    // 심볼 → 이름 캐시(과금/레이트리밋 보호)
//    private final Map<String, String> nameCache = new ConcurrentHashMap<>();
//
//    public ETFQuoteServiceImpl(
//            @Value("${twelvedata.base-url:https://api.twelvedata.com}") String baseUrl,
//            @Value("${twelvedata.api-key}") String apiKey) {
//        this.client = WebClient.builder().baseUrl(baseUrl).build();
//        this.apiKey = apiKey;
//    }
//
//    /** 외부에서 받은 심볼을 Twelve Data 표기로 정규화. 예) 277630.KS → 277630:KRX */
//    public static String normalizeForTwelveData(String symbol) {
//        if (symbol == null) return null;
//        if (symbol.endsWith(".KS")) return symbol.replace(".KS", ":KRX");
//        return symbol;
//    }
//
//    /** 카드용 정보 일괄 수집: EOD(1day) 종가 + 전일대비 계산 */
//    public List<ETFQuoteRequestDTO> fetchPortfolioBatch(List<String> inputSymbols) {
//        // 1) 정규화 & CSV
//        List<String> normalized = inputSymbols.stream()
//                .map(ETFQuoteServiceImpl::normalizeForTwelveData)
//                .toList();
//        String csv = String.join(",", normalized);
//
//        // 2) /time_series 배치 호출 (최신 2개 일봉)
//        Map<String, ETQuoteFResponseDTO> batch = client.get()
//                .uri(uri -> uri.path("/time_series")
//                        .queryParam("symbol", csv)
//                        .queryParam("interval", "1day")
//                        .queryParam("outputsize", "2")
//                        .queryParam("apikey", apiKey).build())
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<Map<String, ETQuoteFResponseDTO>>() {})
//                .block();
//
//        if (batch == null || batch.isEmpty()) return List.of();
//
//        // 3) 심볼별 DTO 구성
//        Map<String, String> nameMap = enrichNames(inputSymbols); // 선택적(가능한 범위에서만)
//        List<ETFQuoteRequestDTO> result = new ArrayList<>();
//
//        for (int i = 0; i < inputSymbols.size(); i++) {
//            String original = inputSymbols.get(i);         // "QQQM" / "277630.KS"
//            String norm = normalized.get(i);               // "QQQM" / "277630:KRX"
//            ETQuoteFResponseDTO r = batch.get(norm);
//            if (r == null || r.values == null || r.values.isEmpty()) continue;
//
//            // 최신/전일
//            ETQuoteFResponseDTO.Value latest = r.values.get(0);
//            Double close = parseDoubleSafe(latest.close);
//            Double prevClose = null;
//            if (r.values.size() >= 2) prevClose = parseDoubleSafe(r.values.get(1).close);
//
//            // EOD 시각(거래소 TZ → 시스템 TZ)
//            ZonedDateTime asOf = parseAsOf(latest.datetime, r.meta != null ? r.meta.exchangeTimezone : null);
//
//            // 이름/통화/거래소
//            String name = nameMap.getOrDefault(original, null);
//            String currency = r.meta != null ? r.meta.currency : null;
//            String exch = r.meta != null ? r.meta.exchange : null;
//
//            result.add(ETFQuoteRequestDTO.of(
//                    original, norm, name, currency,
//                    close != null ? close : Double.NaN,
//                    prevClose,
//                    exch, asOf
//            ));
//        }
//        return result;
//    }
//
//    private static Double parseDoubleSafe(String s) {
//        try { return s == null ? null : Double.valueOf(s); }
//        catch (Exception e) { return null; }
//    }
//
//    private static ZonedDateTime parseAsOf(String date, String exchangeTz) {
//        // time_series 1day는 보통 "yyyy-MM-dd". 거래소 TZ 기준 EOD → 로컬로 변환
//        LocalDate d = LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
//        ZoneId zone = (exchangeTz != null ? ZoneId.of(exchangeTz) : ZoneId.of("UTC"));
//        // EOD 시각을 23:59:59로 가정
//        ZonedDateTime eod = d.atTime(23,59,59).atZone(zone);
//        return eod.withZoneSameInstant(ZoneId.systemDefault());
//    }
//
//    /** 이름 보강: US ETF → /etf, KRX → /symbol_search (가능한 경우에만) */
//    private Map<String, String> enrichNames(List<String> originals) {
//        Map<String, String> out = new HashMap<>();
//        for (String orig : originals) {
//            if (nameCache.containsKey(orig)) { out.put(orig, nameCache.get(orig)); continue; }
//            String norm = normalizeForTwelveData(orig);
//
//            String name = null;
//            if (!norm.endsWith(":KRX")) {
//                // 미국 ETF: /etf?symbol=QQQM
//                name = tryFetchEtfName(norm);
//            } else {
//                // KRX: /symbol_search?symbol=277630&exchange=KRX
//                name = trySearchNameForKrx(norm.split(":")[0]);
//            }
//            if (name != null) { nameCache.put(orig, name); out.put(orig, name); }
//        }
//        return out;
//    }
//
//    private String tryFetchEtfName(String symbol) {
//        try {
//            Map<String, Object> res = client.get().uri(uri -> uri.path("/etf")
//                            .queryParam("symbol", symbol)
//                            .queryParam("apikey", apiKey).build())
//                    .retrieve()
//                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                    .block();
//            // 응답 예: { "data": [ { "symbol":"QQQM", "name":"Invesco NASDAQ 100 ETF", ... } ] }
//            if (res != null && res.containsKey("data")) {
//                List<?> data = (List<?>) res.get("data");
//                if (!data.isEmpty()) {
//                    Object first = data.get(0);
//                    if (first instanceof Map) {
//                        Object name = ((Map<?, ?>) first).get("name");
//                        return name != null ? name.toString() : null;
//                    }
//                }
//            }
//        } catch (Exception ignore) {}
//        return null;
//    }
//
//    private String trySearchNameForKrx(String numericTicker) {
//        try {
//            Map<String, Object> res = client.get().uri(uri -> uri.path("/symbol_search")
//                            .queryParam("symbol", numericTicker)
//                            .queryParam("exchange", "KRX")
//                            .queryParam("apikey", apiKey).build())
//                    .retrieve()
//                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
//                    .block();
//            // 응답 예: { "data":[ {"symbol":"277630","instrument_name":"TIGER 코스피", ...} ] }
//            if (res != null && res.containsKey("data")) {
//                List<?> data = (List<?>) res.get("data");
//                if (!data.isEmpty()) {
//                    Object first = data.get(0);
//                    if (first instanceof Map) {
//                        Object nm = ((Map<?, ?>) first).get("instrument_name");
//                        return nm != null ? nm.toString() : null;
//                    }
//                }
//            }
//        } catch (Exception ignore) {}
//        return null;
//    }
//}
