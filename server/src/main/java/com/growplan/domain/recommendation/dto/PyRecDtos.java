package com.growplan.domain.recommendation.dto;

import java.util.List;
import java.util.Map;

// Request to FastAPI

public class PyRecDtos {

    // Request to FastAPI
    public static class RecRequest {
        public List<String> assets;
        public double rf = 0.0;
        public int points = 10;
        public int risk_level = 3;
        public boolean use_csv = true;
        public int lookback_years = 3;
    }

    // Response from FastAPI
    public static class RecResponse {
        public Metrics metrics;
        public Map<String, Double> last_prices;
        public Map<String, Double> day_change_pct;
        public String as_of;

        public static class Metrics {
            public double annual_return;
            public double annual_vol;
            public Double sharpe;
            public double max_drawdown;
            public Map<String, Double> weights;
        }
    }
}
