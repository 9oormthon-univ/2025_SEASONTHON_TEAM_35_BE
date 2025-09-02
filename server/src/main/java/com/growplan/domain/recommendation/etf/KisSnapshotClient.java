package com.growplan.domain.recommendation.etf;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class KisSnapshotClient {
    private final KisAuthService auth;

    public KisSnapshotClient(KisAuthService auth) { this.auth = auth; }

    // 국내: 전일 종가(또는 현재가) 스냅샷
    public Mono<BigDecimal> domesticPrevClose(String code6){
        return auth.issueAccessToken().flatMap(token -> {
            if (token == null) return Mono.empty();
            WebClient wc = WebClient.builder().baseUrl(auth.restBase()).build();
            return wc.get().uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("fid_cond_mrkt_div_code", "J")
                            .queryParam("fid_input_iscd", code6)
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey", auth.getAppKey())
                    .header("appsecret", auth.getAppSecret())
                    .header("tr_id", "FHKST01010100")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(this::extractDomesticPrevClose);
        });
    }

    // 해외: 전일 종가(또는 마지막 종가) 스냅샷
    public Mono<BigDecimal> overseasPrevClose(String trKey){ // e.g. "ARCX.SPY"
        String[] p = trKey.split("\\.",2);
        String ex = p[0], sym = p[1];
        return auth.issueAccessToken().flatMap(token -> {
            if (token == null) return Mono.empty();
            WebClient wc = WebClient.builder().baseUrl(auth.restBase()).build();
            return wc.get().uri(uriBuilder -> uriBuilder
                            .path("/uapi/overseas-price/v1/quotations/price")
                            .queryParam("AUTH","")
                            .queryParam("EXCD", ex)
                            .queryParam("SYMB", sym)
                            .build())
                    .header("authorization", "Bearer " + token)
                    .header("appkey", auth.getAppKey())
                    .header("appsecret", auth.getAppSecret())
                    .header("tr_id", "HHDFS00000300")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(this::extractOverseasPrevClose);
        });
    }

    // ── 응답 파서(필드명이 약간씩 다를 수 있어 안전하게 파싱) ──
    @SuppressWarnings("unchecked")
    private BigDecimal extractDomesticPrevClose(Map resp){
        Object out = resp.getOrDefault("output", resp.get("output1"));
        if (out instanceof Map<?,?> o) {
            // 우선순위: 전일종가 → 기준가/종가 → 현재가
            String[] keys = {"prdy_clpr","stck_prdy_clpr","bas_prpr","stck_clpr","stck_prpr"};
            for (String k : keys) {
                Object v = o.get(k);
                BigDecimal b = toBD(v);
                if (b != null) return b;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractOverseasPrevClose(Map resp){
        Object out = resp.getOrDefault("output", resp.get("output1"));
        if (out instanceof Map<?,?> o) {
            // 우선순위: 전일종가 → 종가 → 현재가
            String[] keys = {"prdy_clpr","ovrs_prdy_clpr","clos_prpr","last","ovrs_nmix_prpr","ovrs_prpr"};
            for (String k : keys) {
                BigDecimal b = toBD(o.get(k));
                if (b != null) return b;
            }
        }
        return null;
    }

    private static BigDecimal toBD(Object v){
        try {
            if (v == null) return null;
            if (v instanceof Number n) return new BigDecimal(n.toString());
            String s = v.toString().trim();
            if (s.isEmpty() || "-".equals(s)) return null;
            return new BigDecimal(s.replaceAll(",",""));
        } catch (Exception e){ return null; }
    }
}
