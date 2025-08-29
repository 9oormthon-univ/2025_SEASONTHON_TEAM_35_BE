package com.growplan.domain.recommendation.entity;

import com.growplan.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExceptedReturn extends BaseEntity { // 예상 수익률

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exceptedReturnId;

    private Integer yearAfter; // 1, 2, 3 년 후

    private BigDecimal startAmount; // 시작 금액

    private BigDecimal exceptedAmount; // 예상 금액
}
