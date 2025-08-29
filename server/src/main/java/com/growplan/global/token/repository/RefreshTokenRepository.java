package com.growplan.global.token.repository;

import com.growplan.global.token.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findById(Long memberId);
    void deleteById(Long memberId);
    RefreshToken findByMemberId(Long memberId);
}
