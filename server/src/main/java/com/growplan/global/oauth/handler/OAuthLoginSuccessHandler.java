package com.growplan.global.oauth.handler;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.member.repository.MemberRepository;
import com.growplan.global.jwt.principal.PrincipalDetails;
import com.growplan.global.util.CookieUtil;
import com.growplan.global.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${Jwt.redirect}")
    private String REDIRECT_URI;

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails)authentication.getPrincipal();
        Member member = principalDetails.getMember();

        log.info("oauthloginsuccesshandler 실행됨");

        // 리프레시 토큰 발급
        String refreshToken = jwtUtil.generateRefreshToken(member.getMemberId());

        // response에 cookie로 반환
        Cookie cookie = cookieUtil.createCookie(refreshToken);
        response.addCookie(cookie);

        // 액세스 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(member.getMemberId());

        // 액세스 토큰을담아 리다이렉트 uri 생성
        String redirectUri = setRedirectUri(accessToken, member);

        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private String setRedirectUri(String accessToken, Member member) {
        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String encodedName  = URLEncoder.encode(getMemberName(member), StandardCharsets.UTF_8);
        String redirectUri = REDIRECT_URI + "?access_token=" + encodedToken + "&name=" + encodedName;

        log.info("🔗 최종 Redirect URL: {}", redirectUri); // 로그 추가
        return redirectUri;
    }

    private String getMemberName(Member member) {
        String name = member.getName();
        if (name == null || name.isEmpty()) {
            return "임시 유저";
        }
        return name;
    }
}
