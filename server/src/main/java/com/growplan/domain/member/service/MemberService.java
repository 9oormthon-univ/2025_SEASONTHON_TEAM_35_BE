package com.growplan.domain.member.service;

import com.growplan.domain.member.dto.MemberResponseDTO;
import com.growplan.domain.member.entity.Member;
import jakarta.servlet.http.HttpServletResponse;

public interface MemberService {

    void logout(Member member, HttpServletResponse response);
    MemberResponseDTO.MyPageDTO getMyPage(Member member);
}
