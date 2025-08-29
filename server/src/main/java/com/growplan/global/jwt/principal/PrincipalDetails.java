package com.growplan.global.jwt.principal;

import com.growplan.domain.member.entity.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class PrincipalDetails implements UserDetails, OAuth2User {

    private final Member member;
    private Map<String, Object> attributes;

    // 일반 로그인
    public PrincipalDetails(Member member) {
        this.member = member;
    }

    // OAuth 로그인
    public PrincipalDetails(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")); // 기본 권한 부여
    }

    @Override
    public String getUsername() {
        return member.getMemberId().toString(); // ID를 기반으로 인증
    }

    @Override
    public String getPassword() {
        return null; // 패스워드 사용하지 않음
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public String getName() {
        return null;
    }

//    @Override
//    public String getName() {
//        return member.getNickName(); // OAuth2User의 기본 name 값 설정 (카카오 닉네임 사용)
//    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
