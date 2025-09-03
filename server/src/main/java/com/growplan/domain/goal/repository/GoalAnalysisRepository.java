package com.growplan.domain.goal.repository;

import com.growplan.domain.goal.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalAnalysisRepository extends JpaRepository<Goal, Long> {
}
