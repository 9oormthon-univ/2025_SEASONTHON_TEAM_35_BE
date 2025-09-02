package com.growplan.domain.recommendation.etf;

import com.growplan.domain.recommendation.dto.ETFQuoteResponseDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class ETFQuoteStore {
//
//    // key: 국내 6자리, 해외 EXCD.SYMB
//    private final Map<String, ETFQuoteResponseDTO> cache = new ConcurrentHashMap<>();
//
//    // 메타(표기용) — 필요시 DB/설정으로 이관 가능
//    private static final class Meta {
//        final String symbol, name, currency;
//        Meta(String s, String n, String c) { symbol=s; name=n; currency=c; }
//    }
//    private static final Map<String, Meta> META = Map.of(
//            "277630",    new Meta("277630.KS", "TIGER 코스피", "KRW"),
//            "272910",    new Meta("272910.KS", "ACE 중장기국공채액티브", "KRW"),
//            "ARCX.SPY",  new Meta("SPY", "SPDR S&P 500 ETF", "USD"),
//            "NASD.QQQM", new Meta("QQQM", "Invesco NASDAQ 100 ETF", "USD"),
//            "NASD.IMTB", new Meta("IMTB", "iShares Core 5-10 Year USD Bond ETF", "USD")
//    );
//
//    public void upsertDomestic(String code6, String last, String chg, String pct) {
//        Meta m = META.getOrDefault(code6, new Meta(code6, code6, "KRW"));
//        cache.put(code6, ETFQuoteResponseDTO.builder()
//                .key(code6)
//                .symbol(m.symbol)
//                .name(m.name)
//                .currency(m.currency)
//                .price(parse(last))
//                .change(parse(chg))
//                .changePct(parse(pct))
//                .lastUpdated(Instant.now())
//                .build());
//    }
//
//    public void upsertOverseas(String trKey, String last, String chg, String pct) {
//        Meta m = META.getOrDefault(trKey, new Meta(extractSymbol(trKey), extractSymbol(trKey), "USD"));
//        cache.put(trKey, ETFQuoteResponseDTO.builder()
//                .key(trKey)
//                .symbol(m.symbol)
//                .name(m.name)
//                .currency(m.currency)
//                .price(parse(last))
//                .change(parse(chg))
//                .changePct(parse(pct))
//                .lastUpdated(Instant.now())
//                .build());
//    }
//
//    public List<ETFQuoteResponseDTO> getOrdered(List<String> domesticKeys, Map<String,String> overseasKeys) {
//        List<ETFQuoteResponseDTO> out = new ArrayList<>();
//        domesticKeys.forEach(k -> addIfPresent(out, k));
//        overseasKeys.values().forEach(k -> addIfPresent(out, k));
//        return out;
//    }
//
//    private void addIfPresent(List<ETFQuoteResponseDTO> out, String key) {
//        ETFQuoteResponseDTO q = cache.get(key);
//        if (q != null) out.add(q);
//    }
//
//    private static BigDecimal parse(String s) {
//        try {
//            if (s == null || s.isBlank() || "-".equals(s)) return null;
//            return new BigDecimal(s);
//        } catch (Exception e) { return null; }
//    }
//
//    private static String extractSymbol(String trKey) {
//        int i = trKey.indexOf('.');
//        return (i > 0 && i < trKey.length()-1) ? trKey.substring(i+1) : trKey;
//    }


//    private final Map<String, ETFQuoteResponseDTO> cache = new ConcurrentHashMap<>();
//
//    public void upsertDomestic(String code6, String last, String chg, String pct) {
//        ETFQuoteResponseDTO prev = cache.getOrDefault(code6, ETFQuoteResponseDTO.builder()
//                .key(code6).symbol(code6 + ".KS").name(code6).currency("KRW").build());
//        cache.put(code6, merge(prev, last, chg, pct));
//    }
//
//    public void upsertOverseas(String trKey, String last, String chg, String pct) {
//        // trKey 예: "ARCX.SPY"
//        String sym = trKey.contains(".") ? trKey.substring(trKey.indexOf('.') + 1) : trKey;
//        ETFQuoteResponseDTO prev = cache.getOrDefault(trKey, ETFQuoteResponseDTO.builder()
//                .key(trKey).symbol(sym).name(sym).currency("USD").build());
//        cache.put(trKey, merge(prev, last, chg, pct));
//    }
//
//    public Optional<ETFQuoteResponseDTO> find(String key) {
//        return Optional.ofNullable(cache.get(key));
//    }
//
//    private static ETFQuoteResponseDTO merge(ETFQuoteResponseDTO base, String last, String chg, String pct) {
//        return ETFQuoteResponseDTO.builder()
//                .key(base.getKey())
//                .symbol(base.getSymbol())
//                .name(base.getName())
//                .currency(base.getCurrency())
//                .price(parse(last))
//                .change(parse(chg))
//                .changePct(parse(pct))
//                .lastUpdated(Instant.now())
//                .build();
//    }
//
//    private static BigDecimal parse(String s) {
//        try { return (s==null || s.isBlank() || "-".equals(s)) ? null : new BigDecimal(s); }
//        catch (Exception e) { return null; }
//    }
//
//    public void upsertPrevClose(String key, BigDecimal prevClose){
//        ETFQuoteResponseDTO base = cache.computeIfAbsent(key, ETFQuoteStore::placeholder);
//        cache.put(key, ETFQuoteResponseDTO.builder()
//                .key(base.getKey()).symbol(base.getSymbol())
//                .name(base.getName()).currency(base.getCurrency())
//                .price(prevClose)           // 전일 종가를 price로
//                .change(null).changePct(null)
//                .lastUpdated(null)          // 폐장 표시는 null로 두거나 별도 시간 넣기
//                .dataSource("PREV_CLOSE")
//                .session("CLOSED")
//                .build());
//    }

    // key: 국내 6자리, 해외 EXCD.SYMB (예: "ARCX.SPY")
    private final Map<String, ETFQuoteResponseDTO> cache = new ConcurrentHashMap<>();

    // 표기 메타(이름/통화). 필요시 DB/설정 파일로 이관 가능
    private static final class Meta {
        final String symbol, name, currency;
        Meta(String symbol, String name, String currency) {
            this.symbol = symbol; this.name = name; this.currency = currency;
        }
    }
    private static final Map<String, Meta> META = Map.of(
            "277630",     new Meta("277630.KS", "TIGER 코스피", "KRW"),
            "272910",     new Meta("272910.KS", "ACE 중장기국공채액티브", "KRW"),
            "ARCX.SPY",   new Meta("SPY",       "SPDR S&P 500 ETF", "USD"),
            "NASD.QQQM",  new Meta("QQQM",      "Invesco NASDAQ 100 ETF", "USD"),
            "NASD.IMTB",  new Meta("IMTB",      "iShares Core 5-10 Year USD Bond ETF", "USD")
    );

    // (선택) 부팅 시 플레이스홀더를 미리 심어두면 항상 5개가 즉시 보여집니다.
    public ETFQuoteStore(KisProperties props) {
        props.getDomestic().forEach(k -> cache.putIfAbsent(k, placeholder(k)));
        props.getOverseas().values().forEach(k -> cache.putIfAbsent(k, placeholder(k)));
    }

    public void upsertDomestic(String code6, String last, String chg, String pct) {
        ETFQuoteResponseDTO base = cache.computeIfAbsent(code6, ETFQuoteStore::placeholder);
        cache.put(code6, ETFQuoteResponseDTO.builder()
                .key(base.getKey())
                .symbol(base.getSymbol())
                .name(base.getName())
                .currency(base.getCurrency())
                .price(parse(last))
                .change(parse(chg))
                .changePct(parse(pct))
                .lastUpdated(Instant.now())
                .dataSource("LIVE")     // 실시간 틱
                .session("OPEN")        // 장중
                .build());
    }

    public void upsertOverseas(String trKey, String last, String chg, String pct) {
        ETFQuoteResponseDTO base = cache.computeIfAbsent(trKey, ETFQuoteStore::placeholder);
        cache.put(trKey, ETFQuoteResponseDTO.builder()
                .key(base.getKey())
                .symbol(base.getSymbol())
                .name(base.getName())
                .currency(base.getCurrency())
                .price(parse(last))
                .change(parse(chg))
                .changePct(parse(pct))
                .lastUpdated(Instant.now())
                .dataSource("LIVE")     // 실시간 틱
                .session("OPEN")        // 장중
                .build());
    }

    /** 폐장 시 전일 종가로 채우기 */
    public void upsertPrevClose(String key, BigDecimal prevClose){
        ETFQuoteResponseDTO base = cache.computeIfAbsent(key, ETFQuoteStore::placeholder);
        cache.put(key, ETFQuoteResponseDTO.builder()
                .key(base.getKey())
                .symbol(base.getSymbol())
                .name(base.getName())
                .currency(base.getCurrency())
                .price(prevClose)         // 전일 종가
                .change(null)
                .changePct(null)
                .lastUpdated(null)        // 폐장 표시는 null 유지(원하면 시점 기록 가능)
                .dataSource("PREV_CLOSE") // 데이터 출처 표시
                .session("CLOSED")        // 폐장
                .build());
    }

    /** 캐시 조회 */
    public Optional<ETFQuoteResponseDTO> find(String key) {
        return Optional.ofNullable(cache.get(key));
    }
    public Map<String, ETFQuoteResponseDTO> snapshot() { return Map.copyOf(cache); }

    /** ▶▶ 빠진 부분: 플레이스홀더 생성 로직 */
    private static ETFQuoteResponseDTO placeholder(String key) {
        Meta m = META.getOrDefault(key, inferMeta(key));
        return ETFQuoteResponseDTO.builder()
                .key(key)
                .symbol(m.symbol)
                .name(m.name)
                .currency(m.currency)
                .price(null).change(null).changePct(null)
                .lastUpdated(null)
                .dataSource("PREV_CLOSE") // 초기 표시는 전일가 성격(값 없음)
                .session("CLOSED")
                .build();
    }

    /** META에 없을 때 간단 추론 */
    private static Meta inferMeta(String key) {
        if (key.matches("^\\d{6}$")) {
            return new Meta(key + ".KS", key, "KRW");
        }
        String sym = key.contains(".") ? key.substring(key.indexOf('.') + 1) : key;
        return new Meta(sym, sym, "USD");
    }

    private static BigDecimal parse(String s) {
        try { return (s == null || s.isBlank() || "-".equals(s)) ? null : new BigDecimal(s.replaceAll(",", "")); }
        catch (Exception e) { return null; }
    }
}