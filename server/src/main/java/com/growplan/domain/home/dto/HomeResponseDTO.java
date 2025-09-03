package com.growplan.domain.home.dto;

import com.growplan.domain.goal.dto.GoalResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class HomeResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewDTO {

        private String name;
        private BigDecimal totalAsset;

        // 자산 비율
        private int cashRatio;
        private int depositRatio;
        private int savingRatio;
        private int investRatio;
        private int etcRatio;

        private BigDecimal targetAmount;
//        private int achievementRate;
    }
}
