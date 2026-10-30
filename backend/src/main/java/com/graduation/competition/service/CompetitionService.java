package com.graduation.competition.service;

import com.graduation.competition.common.PageResult;
import com.graduation.competition.dto.CompetitionQuery;
import com.graduation.competition.dto.CompetitionRequest;
import com.graduation.competition.dto.CompetitionResponse;

import java.util.List;

public interface CompetitionService {

    CompetitionResponse create(CompetitionRequest request);

    PageResult<CompetitionResponse> query(CompetitionQuery query);

    CompetitionResponse get(Long id);

    CompetitionResponse update(Long id, CompetitionRequest request);

    void delete(Long id);

    List<CompetitionResponse.RuleResponse> getRules(Long competitionId, boolean activeOnly);

    CompetitionResponse.RuleResponse addRule(Long competitionId, CompetitionRequest.RuleRequest request);

    CompetitionResponse.RuleResponse updateRule(Long ruleId, CompetitionRequest.RuleRequest request);

    CompetitionResponse.GroupResponse getGroup(Long groupId);
}

