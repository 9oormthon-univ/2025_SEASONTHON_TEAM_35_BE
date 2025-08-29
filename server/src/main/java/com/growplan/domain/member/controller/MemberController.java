package com.growplan.domain.member.controller;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.member.service.MemberService;
import com.growplan.global.annotation.CurrentMember;
import com.growplan.global.error.ApiResponse;
import com.growplan.global.jwt.principal.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v0/member")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/info")
    @Operation(summary = "소셜 로그인 설명 API", description = "카카오 로그인 과정 설명입니다. <br />"
            + "1. (백엔드배포주소)/oauth2/authorization/kakao로 연결 <br />"
            + "2. 소셜 로그인 성공 시 프론트엔드에서 설정한 주소로 리다리엑트 됩니다. (주소는 따로 알려주세요) <br />")
    public ApiResponse<Void> signUp() {
        return ApiResponse.onSuccess(null);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃 API", description = "로그아웃 API 입니다.")
    public ApiResponse<Void> logout(@CurrentMember Member member, HttpServletResponse response) {
        memberService.logout(member, response);
        return ApiResponse.onSuccess(null);
    }

    @GetMapping("/success")
    @Operation(summary = "백엔드 테스트용입니다", description = "로그인 테스트용입니다 연결 X")
    public Map<String, Object> success(@RequestParam("access_token") String accessToken) {
        return Map.of(
                "message", "OAuth2 login success",
                "access_token", accessToken
        );
    }

    @GetMapping("/me")
    @Operation(summary = "백엔드 테스트용입니다", description = "로그인 테스트용입니다 연결 X")
    public Map<String, Object> me(@AuthenticationPrincipal PrincipalDetails principal) {
        return Map.of(
                "memberId", principal.getMember().getMemberId(),
                "name", principal.getMember().getName(),
                "email", principal.getMember().getEmail()
        );
    }
}
