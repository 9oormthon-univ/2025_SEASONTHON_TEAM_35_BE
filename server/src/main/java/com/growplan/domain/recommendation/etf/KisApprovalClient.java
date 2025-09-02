package com.growplan.domain.recommendation.etf;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class KisApprovalClient {

    private final WebClient webClient;
    private final String base;

    public KisApprovalClient(@Value("${kis.env}") String env) {
        // 실전(prod) / 모의(paper) 도메인
        this.base = "prod".equalsIgnoreCase(env)
                ? "https://openapi.koreainvestment.com:9443"
                : "https://openapivts.koreainvestment.com:29443";
        this.webClient = WebClient.builder().baseUrl(base).build();
    }

    public Mono<String> issueApprovalKey(String appKey, String appSecret) {
        record Req(@JsonProperty("grant_type") String grantType,
                   @JsonProperty("appkey") String appKey,
                   @JsonProperty("secretkey") String secretKey) {}
        record Res(@JsonProperty("approval_key") String approvalKey) {}

        return webClient.post()
                .uri("/oauth2/Approval")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Req("client_credentials", appKey, appSecret))
                .retrieve()
                .bodyToMono(Res.class)
                .map(Res::approvalKey);
    }

    public String getBase() { return base; }
}
