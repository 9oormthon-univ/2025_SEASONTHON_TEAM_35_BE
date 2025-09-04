package com.growplan.global.config;


import com.growplan.domain.recommendation.dto.PyRecDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class PyRecClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${python.recommender.base-url}")
    private String baseUrl;

    public PyRecDtos.RecResponse recommend(PyRecDtos.RecRequest req) {
        return webClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(PyRecDtos.RecResponse.class)
                .block();
    }
}
