package com.growplan.domain.recommendation.service;

import com.growplan.domain.recommendation.dto.ETFQuoteResponseDTO;
import com.growplan.domain.recommendation.etf.ETFQuoteStore;
import com.growplan.domain.recommendation.etf.KisProperties;
import com.growplan.domain.recommendation.etf.KisSnapshotClient;
import com.growplan.domain.recommendation.etf.MarketClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ETFQuoteQueryService {

//    private final ETFQuoteStore store;
//    private final KisProperties props;
//
//    public List<ETFQuoteResponseDTO> currentQuotesEnsureAll() {
//        // 1) 대상 키 전체(국내 6자리 + 해외 trKey)
//        List<String> targets = new ArrayList<>(props.getDomestic());
//        targets.addAll(props.getOverseas().values());
//
//        // 2) 메타(표기용) — 필요시 이름은 자유롭게 수정
//        Map<String, ETFQuoteResponseDTO> meta = new HashMap<>();
//        meta.put("277630", dto("277630", "277630.KS", "TIGER 코스피", "KRW"));
//        meta.put("272910", dto("272910", "272910.KS", "ACE 중장기국공채액티브", "KRW"));
//        meta.put("ARCX.SPY",  dto("ARCX.SPY",  "SPY",  "SPDR S&P 500 ETF", "USD"));
//        meta.put("NASD.QQQM", dto("NASD.QQQM", "QQQM", "Invesco NASDAQ 100 ETF", "USD"));
//        meta.put("NASD.IMTB", dto("NASD.IMTB", "IMTB", "iShares Core 5-10 Year USD Bond ETF", "USD"));
//
//        // 3) 캐시 있으면 사용, 없으면 메타로 “빈 슬롯” 채워 반환
//        return targets.stream().map(k ->
//                store.find(k).orElseGet(() -> {
//                    ETFQuoteResponseDTO m = meta.getOrDefault(k, dto(k, k, k, inferCcy(k)));
//                    // 빈 슬롯은 시간만 넣어 UI에 '갱신 대기' 표기 가능
//                    m.setLastUpdated(null);
//                    return m;
//                })
//        ).collect(Collectors.toList());
//    }
//
//    private static ETFQuoteResponseDTO dto(String key, String symbol, String name, String ccy) {
//        return ETFQuoteResponseDTO.builder().key(key).symbol(symbol).name(name).currency(ccy).build();
//    }
//    private static String inferCcy(String key) { return key.matches("^\\d{6}$") ? "KRW" : "USD"; }

    private final ETFQuoteStore store;
    private final KisProperties props;
    private final KisSnapshotClient snapshot;

    public List<ETFQuoteResponseDTO> currentQuotesEnsureAll() {
        List<String> allKeys = new ArrayList<>(props.getDomestic());
        allKeys.addAll(props.getOverseas().values());

        // 1) 플레이스홀더 보장
        allKeys.forEach(k -> store.find(k).orElseGet(() -> {
            var p = placeholder(k); store.upsertPrevClose(k, null); return p; // seed 용
        }));

        // 2) 장 상태에 따라 보강(동기 블로킹 호출로 간단 처리)
//        for (String key : allKeys) {
//            boolean isKr = MarketClock.isDomesticKey(key);
//            var session = isKr ? MarketClock.krxSessionNow() : MarketClock.usSessionNow();
//
//            var cached = store.find(key).orElse(null);
//            boolean hasLive = (cached != null && cached.getPrice() != null && "LIVE".equals(cached.getDataSource()));
//
//            if (session == MarketClock.Session.CLOSED && !hasLive) {
//                // 폐장 + 라이브 없음 → 전일 종가 스냅샷
//                BigDecimal prev = isKr
//                        ? snapshot.domesticPrevClose(key).block()
//                        : snapshot.overseasPrevClose(key).block();
//                if (prev != null) store.upsertPrevClose(key, prev);
//            }
//        }
        for (String key : allKeys) {
            boolean isKr = MarketClock.isDomesticKey(key);
            var session = isKr ? MarketClock.krxSessionNow() : MarketClock.usSessionNow();

            var cached = store.find(key).orElse(null);
            boolean hasLive = (cached != null && cached.getPrice() != null && "LIVE".equals(cached.getDataSource()));

            if (session == MarketClock.Session.CLOSED && !hasLive) {
                try {
                    BigDecimal prev = isKr
                            ? snapshot.domesticPrevClose(key).block(java.time.Duration.ofSeconds(2))
                            : snapshot.overseasPrevClose(key).block(java.time.Duration.ofSeconds(2));
                    if (prev != null) store.upsertPrevClose(key, prev);
                } catch (Exception ignore) {
                    // ★ 403 등 모든 예외를 흡수 → 응답은 계속 200
                }
            }
        }

        // 3) 최종 재생성(방금 채운 값 반영)
        return allKeys.stream()
                .map(k -> store.find(k).orElseGet(() -> placeholder(k)))
                .peek(q -> {
                    boolean isKr = MarketClock.isDomesticKey(q.getKey());
                    q.setSession((isKr ? MarketClock.krxSessionNow() : MarketClock.usSessionNow()).name());
                    if (q.getDataSource() == null) q.setDataSource(q.getLastUpdated()==null ? "PREV_CLOSE" : "LIVE");
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private static ETFQuoteResponseDTO placeholder(String key){
        // store.placeholder와 동일 로직 사용 권장(중복 시 제거)
        String ccy = key.matches("^\\d{6}$") ? "KRW" : "USD";
        String symbol = key.matches("^\\d{6}$") ? key + ".KS"
                : (key.contains(".") ? key.substring(key.indexOf('.')+1) : key);
        return ETFQuoteResponseDTO.builder()
                .key(key).symbol(symbol).name(symbol).currency(ccy)
                .dataSource("PREV_CLOSE").session("CLOSED")
                .build();
    }
}
