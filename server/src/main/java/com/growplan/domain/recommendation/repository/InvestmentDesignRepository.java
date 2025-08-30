package com.growplan.domain.recommendation.repository;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.recommendation.entity.InvestmentDesign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestmentDesignRepository extends JpaRepository<InvestmentDesign, Long> {

    Optional<InvestmentDesign> findByMember(Member member);
}
