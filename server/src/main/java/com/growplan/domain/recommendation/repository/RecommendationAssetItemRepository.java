package com.growplan.domain.recommendation.repository;

import com.growplan.domain.recommendation.entity.RecommendAsseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationAssetItemRepository extends JpaRepository<RecommendAsseItem, Long> {
}
