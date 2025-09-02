package com.growplan.domain.recommendation.etf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KisEndpoints {

    private final String env;
    private final boolean wsSecure;

    public KisEndpoints(@Value("${kis.env}") String env,
                        @Value("${kis.ws.secure:false}") boolean wsSecure) {
        this.env = env;
        this.wsSecure = wsSecure;
    }

    public String restBase() {
        return "prod".equalsIgnoreCase(env)
                ? "https://openapi.koreainvestment.com:9443"
                : "https://openapivts.koreainvestment.com:29443";
    }

    public String wsUrl() {
        String scheme = wsSecure ? "wss" : "ws";
        int port = "prod".equalsIgnoreCase(env) ? 21000 : 31000;
        return String.format("%s://ops.koreainvestment.com:%d/tryitout", scheme, port);
    }

    public boolean isProd() { return "prod".equalsIgnoreCase(env); }
}
