package com.growplan.domain.recommendation.etf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class KisAuthService {
    private static final Logger log = LoggerFactory.getLogger(KisAuthService.class);
    private final WebClient webClient;
    private final String appKey, appSecret;
    private final KisEndpoints kisEndpoints;
    private volatile String cachedToken;
    private volatile long   tokenExpiryEpochMs;

//    public KisAuthService(@Value("${kis.env}") String env,
//                          @Value("${kis.appkey}") String appKey,
//                          @Value("${kis.secretkey}") String appSecret) {
//        this.base = "prod".equalsIgnoreCase(env)
//                ? "https://openapi.koreainvestment.com:9443"
//                : "https://openapivts.koreainvestment.com:29443";
//        this.webClient = WebClient.builder().baseUrl(base).build();
//        this.appKey = appKey; this.appSecret = appSecret;
//    }

    public KisAuthService(KisEndpoints kisEndpoints,
                          KisProperties props) {
        this.kisEndpoints = kisEndpoints;
        this.webClient = WebClient.builder().baseUrl(kisEndpoints.restBase()).build(); // REST 전용
        this.appKey = props.getAppkey();
        this.appSecret = props.getSecretkey();
    }

    public Mono<String> issueAccessToken() {
        return webClient.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "grant_type", "client_credentials",
                        "appkey", appKey,
                        "appsecret", appSecret
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).flatMap(body -> {
                            log.warn("[KIS] tokenP {} - {}", resp.statusCode(), body);
                            return Mono.error(new RuntimeException("tokenP failed: " + resp.statusCode()));
                        })
                )
                .bodyToMono(Map.class)
                .map(m -> {
                    String token = (String) m.get("access_token");
                    Object exp   = m.getOrDefault("expires_in", 0);
                    long sec = 0;
                    try { sec = Long.parseLong(String.valueOf(exp)); } catch (Exception ignored) {}
                    cachedToken = token;
                    tokenExpiryEpochMs = System.currentTimeMillis() + sec * 1000L;
                    return token;
                })
                .onErrorResume(e -> {
                    // ★ 여기서 빈 Mono로 흡수 → 스냅샷은 건너뛰되, 전체 응답은 정상 유지
                    log.warn("[KIS] issueAccessToken error: {}", e.toString());
                    return Mono.empty();
                });
    }

    public String restBase(){ return kisEndpoints.restBase(); }
    public String wsUrl(){ return kisEndpoints.wsUrl(); }
    public String getAppKey(){ return appKey; }
    public String getAppSecret(){ return appSecret; }
}
