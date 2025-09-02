package com.growplan.domain.member.repository;

import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);
    AssetPortfolio findTotalAmountByMemberId(Member member);
}
