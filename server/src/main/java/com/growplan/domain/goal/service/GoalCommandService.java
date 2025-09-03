package com.growplan.domain.goal.service;

import com.growplan.domain.goal.dto.GoalResponseDTO;
import com.growplan.domain.member.entity.Member;

public interface GoalCommandService {

    GoalResponseDTO.GoalAnalysisResponseDTO analyzeGoal(Member member);
}
