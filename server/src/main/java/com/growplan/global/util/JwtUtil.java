package com.growplan.global.util;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.member.repository.MemberRepository;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.GeneralException;
import com.growplan.global.token.entity.RefreshToken;
import com.growplan.global.token.repository.RefreshTokenRepository;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final MemberRepository memberRepository;

    private SecretKey secretKey;
    private long accessTokenExpirationTime;
    private long refreshTokenExpirationTime;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtUtil(MemberRepository memberRepository, @Value("${Jwt.secret}") String secret,
                   @Value("${Jwt.access-token.expiration-time}") long accessExpiration,
                   @Value("${Jwt.refresh-token.expiration-time}") long refreshExpiration,
                   RefreshTokenRepository refreshTokenRepository) {
        this.memberRepository = memberRepository;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationTime = accessExpiration; // Access 토큰 만료 시간 설정
        this.refreshTokenExpirationTime = refreshExpiration; // Refresh 토큰 만료 시간 설정
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // 액세스 토큰을 발급하는 메서드
    public String generateAccessToken(Long memberId) {
        return Jwts.builder()
                .claim("memberId", memberId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationTime))
                .signWith(secretKey)
                .compact();
    }

//    @Transactional
//    // 리프레쉬 토큰을 발급하는 메서드
//    public String generateRefreshToken(Long memberId) {
//        // 기존 리프레시 토큰 삭제
//        refreshTokenRepository.deleteById(memberId);
//
//        // 새 리프레시 토큰 발급 후 저장
//        String refreshToken = Jwts.builder()
//                .claim("memberId", memberId)
//                .issuedAt(new Date())
//                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime))
//                .signWith(secretKey)
//                .compact();
//        refreshTokenRepository.save(new RefreshToken(memberId, refreshToken));
//        return refreshToken;
//    }

    @Transactional
    public String generateRefreshToken(Long memberId) {
        RefreshToken existingRefreshToken = refreshTokenRepository.findByMemberId(memberId); // memberId로 RefreshToken 조회
        log.info("기존 RefreshToken 조회 결과: {}", existingRefreshToken);

        String refreshToken = Jwts.builder()
                .claim("memberId", memberId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationTime))
                .signWith(secretKey)
                .compact();

        if (existingRefreshToken != null) {
            log.info("기존 RefreshToken이 존재함: {}", existingRefreshToken.getRefreshToken());
            // 기존 RefreshToken이 존재하면 업데이트
            existingRefreshToken.setRefreshToken(refreshToken); // RefreshToken 값 업데이트
            // existingRefreshToken은 영속성 컨텍스트에 의해 자동으로 업데이트됨 (따라서 save() 불필요)
            refreshTokenRepository.save(existingRefreshToken);
        } else {
            log.info("새로운 RefreshToken 생성: {}", refreshToken);
            // 기존 RefreshToken이 존재하지 않으면 새로 생성하여 저장
            refreshTokenRepository.save(new RefreshToken(memberId, refreshToken));
        }
        log.info("RefreshToken 저장 완료");
        return refreshToken;
    }


    // 응답 헤더에서 액세스 토큰을 반환하는 메서드
    public String getAccessTokenFromHeader(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.split(" ")[1];
    }

    // 토큰에서 유저 id를 반환하는 메서드
    public Long getIdFromToken(String accessToken) {
        try {
            Long memberId = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload()
                    .get("memberId", Long.class);

            return memberId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }
    }


    // 토큰에서 멤버를 반환하는 메서드
    public Member getMemberFromHeader(String authorizationHeader) { // annotation 추가 시 이용
        String token = getAccessTokenFromHeader(authorizationHeader);
        Long id = getIdFromToken(token);

        return memberRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    // Jwt 토큰의 유효기간을 확인하는 메서드
    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expirationDate.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰이 유효하지 않은 경우
            throw new GeneralException(ErrorStatus.EXPIRED_TOKEN);
        }
    }
}
