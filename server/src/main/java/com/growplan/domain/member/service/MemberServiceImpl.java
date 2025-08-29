package com.growplan.domain.member.service;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.member.repository.MemberRepository;
import com.growplan.global.token.repository.RefreshTokenRepository;
import com.growplan.global.util.CookieUtil;
import com.growplan.global.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    private void setRefreshToken(Long memberId, HttpServletResponse response) {
        // 새 RefreshToken 발급
        String refreshToken = jwtUtil.generateRefreshToken(memberId);

        // response에 cookie로 반환
        Cookie cookie = cookieUtil.createCookie(refreshToken);
        response.addCookie(cookie);
    }

    @Override
    public void logout(Member member, HttpServletResponse response) {

        refreshTokenRepository.deleteById(member.getMemberId());

        Cookie deletedCookie = cookieUtil.deleteCookie();
        response.addCookie(deletedCookie);
    }
}
