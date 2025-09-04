package com.growplan.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class InvestApiConfig {

    @Bean
    public WebClient recommendWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://two025-seasonthon-team-35-be.onrender.com")
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }
}
