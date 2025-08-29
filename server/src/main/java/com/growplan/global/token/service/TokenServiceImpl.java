package com.growplan.global.token.service;

import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.GeneralException;
import com.growplan.global.token.converter.TokenConverter;
import com.growplan.global.token.dto.TokenResponseDTO;
import com.growplan.global.token.entity.RefreshToken;
import com.growplan.global.token.repository.RefreshTokenRepository;
import com.growplan.global.util.CookieUtil;
import com.growplan.global.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    // 리프레쉬 토큰을 재발행하는 메서드
    @Override
    public TokenResponseDTO.TokenDTO reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = cookieUtil.getCookie(request);
        String refreshToken = cookie.getValue();
        Long id = jwtUtil.getIdFromToken(refreshToken);
        RefreshToken existRefreshToken = refreshTokenRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.REFRESH_TOKEN_NOT_FOUND));

        String newAccessToken;
        if (!existRefreshToken.getRefreshToken().equals(refreshToken) || jwtUtil.isTokenExpired(refreshToken)) {
            // 리프레쉬 토큰이 다르거나, 만료된 경우, 재로그인 필요
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        } else {
            // 액세스 토큰 재발급
            newAccessToken = jwtUtil.generateAccessToken(id);
        }

        // 새로운 리프레쉬 토큰 발급 및 저장
        String newRefreshToken = jwtUtil.generateRefreshToken(id);
        RefreshToken updatedRefreshToken = new RefreshToken(id, newRefreshToken);
        refreshTokenRepository.save(updatedRefreshToken);

        // 리프레쉬 토큰이 담긴 쿠키 생성 후 설정
        Cookie newCookie = cookieUtil.createCookie(newRefreshToken);
        response.addCookie(newCookie);

        // 새로운 액세스 토큰을 담아 반환
        return TokenConverter.toTokenDTO(newAccessToken);
    }
}
