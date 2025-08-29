package com.growplan.global.oauth.service;

import com.growplan.domain.member.entity.Member;
import com.growplan.domain.member.repository.MemberRepository;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.GeneralException;
import com.growplan.global.jwt.principal.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PrincipalDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        return new PrincipalDetails(member);
    }
}
