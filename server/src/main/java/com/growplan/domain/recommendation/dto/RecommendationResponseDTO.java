package com.growplan.domain.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

public class RecommendationResponseDTO {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvestmentSummaryResponse {

        @JsonProperty("etfList")
        private List<ETFQuoteResponseDTO> etfList;
    }
}
