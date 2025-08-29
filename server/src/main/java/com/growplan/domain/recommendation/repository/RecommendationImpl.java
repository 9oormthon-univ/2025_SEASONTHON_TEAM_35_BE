package com.growplan.domain.recommendation.repository;

import com.growplan.domain.recommendation.entity.QRecommendation;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecommendationImpl implements RecommendationCustom { // QueryDSL template

    private final JPAQueryFactory jpaQueryFactory;
    private final QRecommendation recommendation = QRecommendation.recommendation;
}
