package com.growplan.domain.member.converter;

import com.growplan.domain.member.dto.MemberResponseDTO;
import com.growplan.domain.member.entity.Member;

public class MemberConverter {


    public static Member toSocialMember(String provider, String providerId, String name, String email) {

        return Member.builder()
                .provider(provider)
                .providerId(providerId)
                .name(name)
                .email(email)
                .build();
    }

    public static MemberResponseDTO.LoginSuccessDTO toLoginSuccessDTO(String accessToken) {
        return MemberResponseDTO.LoginSuccessDTO.builder()
                .accessToken(accessToken)
                .build();
    }

}
