package com.growplan.domain.recommendation.repository;

import com.growplan.domain.recommendation.entity.ExceptedReturn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpectedReturnRepository extends JpaRepository<ExceptedReturn, Long> {
}
