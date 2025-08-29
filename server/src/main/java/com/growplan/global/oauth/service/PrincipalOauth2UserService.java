package com.growplan.global.oauth.service;

import com.growplan.domain.member.converter.MemberConverter;
import com.growplan.domain.member.entity.Member;
import com.growplan.domain.member.repository.MemberRepository;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.GeneralException;
import com.growplan.global.jwt.principal.PrincipalDetails;
import com.growplan.global.oauth.userinfo.KakaoUserInfo;
import com.growplan.global.oauth.userinfo.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    // 소셜로그인 유저 처리
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        log.info("OAuth2 로그인 요청: provider={}, attributes={}", provider, oAuth2User.getAttributes());

        OAuth2UserInfo userInfo = getOAuth2UserInfo(provider, oAuth2User.getAttributes());

        // 기존 정보가 있으면 로그인, 없는 경우 임시 회원가입
        Member member = saveOrUpdateMember(userInfo);

        // OAuth2User 반환
        return new PrincipalDetails(member, oAuth2User.getAttributes());
    }

    private OAuth2UserInfo getOAuth2UserInfo(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "kakao" -> new KakaoUserInfo(attributes);
            default -> throw new GeneralException(ErrorStatus.INVALID_PROVIDER);
        };
    }

    private Member saveOrUpdateMember(OAuth2UserInfo userInfo) {
        String provider = userInfo.getProvider();
        String providerId = userInfo.getProviderId();
        String name = userInfo.getName();
        String email = userInfo.getEmail();

        Optional<Member> optionalMember = memberRepository.findByProviderAndProviderId(provider, providerId);

        if (optionalMember.isEmpty()) {
            Member member = MemberConverter.toSocialMember(provider, providerId, name, email);
            return memberRepository.save(member);
        }

        return optionalMember.get();
    }
}
