package com.growplan.domain.member.converter;

import com.growplan.domain.asset.entity.AssetPortfolio;
import com.growplan.domain.member.dto.MemberResponseDTO;
import com.growplan.domain.member.entity.Member;

import java.math.BigDecimal;

public class MemberConverter {


    public static Member toSocialMember(String provider, String providerId, String name, String email) {

        return Member.builder()
                .provider(provider)
                .providerId(providerId)
                .name(name)
                .email(email)
                .build();
    }

    public static MemberResponseDTO.LoginSuccessDTO toLoginSuccessDTO(String accessToken, String name) {
        return MemberResponseDTO.LoginSuccessDTO.builder()
                .accessToken(accessToken)
                .name(name)
                .build();
    }

    public static MemberResponseDTO.MyPageDTO toMyPageDTO(Member member, BigDecimal totalAmount) {
        return MemberResponseDTO.MyPageDTO.builder()
                .name(member.getName())
                .email(member.getEmail())
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .build();
    }

    public static MemberResponseDTO.getNameDTO toNameDTO(Member member) {
        return MemberResponseDTO.getNameDTO.builder()
                .name(member.getName())
                .build();
    }

}
