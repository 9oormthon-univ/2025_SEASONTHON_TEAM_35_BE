package com.growplan.domain.home.service;

import com.growplan.domain.home.dto.HomeResponseDTO;
import com.growplan.domain.member.entity.Member;

public interface HomeService {

    HomeResponseDTO.OverviewDTO getOverview(Member member);
}
