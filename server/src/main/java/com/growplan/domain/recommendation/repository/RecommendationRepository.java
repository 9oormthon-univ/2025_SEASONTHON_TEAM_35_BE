package com.growplan.domain.recommendation.repository;

import com.growplan.domain.recommendation.entity.InvestmentDesign;
import com.growplan.domain.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByDesign(InvestmentDesign design);
}
