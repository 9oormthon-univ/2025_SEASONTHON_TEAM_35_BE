package com.growplan.domain.asset.repository;

import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetPortfolioRepository extends JpaRepository<AssetPortfolio, Long> {

    Optional<AssetPortfolio> findByMember(Member member);
}
