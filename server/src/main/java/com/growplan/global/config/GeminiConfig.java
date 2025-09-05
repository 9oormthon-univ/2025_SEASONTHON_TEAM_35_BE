package com.growplan.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GeminiConfig {

    @Bean
    public WebClient geminiWebClient(
            WebClient.Builder builder,
            @Value("${gemini.api-key}") String apiKey
    ) {
        return builder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                // Google Generative Language API는 헤더 x-goog-api-key 또는 ?key= 를 허용
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }
}
