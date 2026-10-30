package com.graduation.competition.service.impl;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import com.graduation.competition.common.PageResult;
import com.graduation.competition.dto.CompetitionQuery;
import com.graduation.competition.dto.CompetitionRequest;
import com.graduation.competition.dto.CompetitionResponse;
import com.graduation.competition.entity.Competition;
import com.graduation.competition.entity.CompetitionGroup;
import com.graduation.competition.entity.CompetitionRule;
import com.graduation.competition.repository.CompetitionGroupRepository;
import com.graduation.competition.repository.CompetitionRepository;
import com.graduation.competition.repository.CompetitionRuleRepository;
import com.graduation.competition.service.CompetitionService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 竞赛信息服务。所有删除操作均为软删除，已结束数据仍保留用于审计。
 */
@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionGroupRepository groupRepository;
    private final CompetitionRuleRepository ruleRepository;

    @Override
    @Transactional
    public CompetitionResponse create(CompetitionRequest request) {
        Competition competition = new Competition();
        apply(competition, request);
        appendChildren(competition, request);
        return toResponse(competitionRepository.save(competition), true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CompetitionResponse> query(CompetitionQuery query) {
        Specification<Competition> specification = (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));
            if (StringUtils.hasText(query.getName())) {
                predicates.add(builder.like(builder.lower(root.get("name")),
                        "%" + query.getName().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(query.getOrganizer())) {
                predicates.add(builder.like(builder.lower(root.get("organizer")),
                        "%" + query.getOrganizer().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(query.getCategory())) {
                predicates.add(builder.equal(root.get("category"), query.getCategory()));
            }
            if (StringUtils.hasText(query.getLevel())) {
                predicates.add(builder.equal(root.get("level"), query.getLevel()));
            }
            if (StringUtils.hasText(query.getStatus())) {
                String status = query.getStatus().toUpperCase(Locale.ROOT);
                if ("EXPIRED".equals(status)) {
                    predicates.add(builder.or(
                            builder.equal(root.get("status"), status),
                            builder.lessThan(root.get("contestEnd"), LocalDate.now())));
                } else {
                    predicates.add(builder.equal(root.get("status"), status));
                }
            }
            if (query.getDateFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("contestStart"), query.getDateFrom()));
            }
            if (query.getDateTo() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("contestStart"), query.getDateTo()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<Competition> page = competitionRepository.findAll(
                specification,
                PageRequest.of(query.getPage() - 1, query.getSize(),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        List<CompetitionResponse> records = page.getContent().stream()
                .map(competition -> toResponse(competition, false))
                .toList();
        return new PageResult<>(records, page.getTotalElements(), query.getPage(), query.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionResponse get(Long id) {
        return toResponse(requireDetails(id), true);
    }

    @Override
    @Transactional
    public CompetitionResponse update(Long id, CompetitionRequest request) {
        Competition competition = requireDetails(id);
        apply(competition, request);
        competition.getGroups().clear();
        competition.getRules().clear();
        appendChildren(competition, request);
        return toResponse(competitionRepository.save(competition), true);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Competition competition = requireDetails(id);
        competition.setDeleted(true);
        competition.getGroups().forEach(group -> group.setDeleted(true));
        competition.getRules().forEach(rule -> rule.setDeleted(true));
        competitionRepository.save(competition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetitionResponse.RuleResponse> getRules(Long competitionId, boolean activeOnly) {
        if (!competitionRepository.findByIdAndDeletedFalse(competitionId).isPresent()) {
            throw new BizException(ErrorCode.RESOURCE_NOT_FOUND, "竞赛不存在");
        }
        return ruleRepository.findRules(competitionId, activeOnly, LocalDate.now()).stream()
                .map(rule -> toRuleResponse(rule, effectiveStatus(rule.getCompetition())))
                .toList();
    }

    @Override
    @Transactional
    public CompetitionResponse.RuleResponse addRule(Long competitionId, CompetitionRequest.RuleRequest request) {
        Competition competition = requireDetails(competitionId);
        CompetitionRule rule = new CompetitionRule();
        applyRule(competition, rule, request);
        ruleRepository.save(rule);
        return toRuleResponse(rule, effectiveStatus(competition));
    }

    @Override
    @Transactional
    public CompetitionResponse.RuleResponse updateRule(Long ruleId, CompetitionRequest.RuleRequest request) {
        CompetitionRule rule = ruleRepository.findByIdAndDeletedFalse(ruleId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "规则不存在"));
        applyRule(rule.getCompetition(), rule, request);
        ruleRepository.save(rule);
        return toRuleResponse(rule, effectiveStatus(rule.getCompetition()));
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionResponse.GroupResponse getGroup(Long groupId) {
        CompetitionGroup group = groupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "组别不存在"));
        return new CompetitionResponse.GroupResponse(group.getId(), group.getName(), group.getEligibility());
    }

    private Competition requireDetails(Long id) {
        return competitionRepository.findDetailsById(id)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "竞赛不存在"));
    }

    private void apply(Competition competition, CompetitionRequest request) {
        validateDates(request);
        competition.setName(request.name().trim());
        competition.setOrganizer(request.organizer());
        competition.setCategory(request.category());
        competition.setLevel(request.level());
        competition.setOfficialWebsite(request.officialWebsite());
        competition.setRegisterStart(request.registerStart());
        competition.setRegisterEnd(request.registerEnd());
        competition.setContestStart(request.contestStart());
        competition.setContestEnd(request.contestEnd());
        competition.setDescription(request.description());
        competition.setSourceUrl(request.sourceUrl().trim());
        competition.setSourceDate(request.sourceDate());
        competition.setStatus(StringUtils.hasText(request.status())
                ? request.status().toUpperCase(Locale.ROOT) : "ACTIVE");
    }

    private void validateDates(CompetitionRequest request) {
        if (request.sourceDate().isAfter(LocalDate.now())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "来源发布日期不能晚于今天");
        }
        if (request.registerStart() != null && request.registerEnd() != null
                && request.registerStart().isAfter(request.registerEnd())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "报名开始日期不能晚于报名截止日期");
        }
        if (request.contestStart() != null && request.contestEnd() != null
                && request.contestStart().isAfter(request.contestEnd())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "比赛开始日期不能晚于比赛结束日期");
        }
    }

    private void appendChildren(Competition competition, CompetitionRequest request) {
        if (request.groups() != null) {
            request.groups().forEach(groupRequest -> {
                CompetitionGroup group = new CompetitionGroup();
                group.setName(groupRequest.name().trim());
                group.setEligibility(groupRequest.eligibility());
                competition.addGroup(group);
            });
        }
        if (request.rules() != null) {
            request.rules().forEach(ruleRequest -> {
                CompetitionRule rule = new CompetitionRule();
                applyRule(competition, rule, ruleRequest);
                competition.addRule(rule);
            });
        }
    }

    private void applyRule(
            Competition competition,
            CompetitionRule rule,
            CompetitionRequest.RuleRequest request) {
        if (request.sourceDate().isAfter(LocalDate.now())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "规则来源发布日期不能晚于今天");
        }
        rule.setTitle(request.title().trim());
        rule.setContent(request.content());
        rule.setSourceUrl(request.sourceUrl().trim());
        rule.setSourceDate(request.sourceDate());
        rule.setPublishDate(request.publishDate());
        rule.setStatus(StringUtils.hasText(request.status())
                ? request.status().toUpperCase(Locale.ROOT) : "ACTIVE");
        if (request.groupId() != null) {
            CompetitionGroup group = competition.getGroups().stream()
                    .filter(candidate -> Objects.equals(candidate.getId(), request.groupId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, "规则关联的组别不存在"));
            rule.setGroup(group);
        }
    }

    private CompetitionResponse toResponse(Competition competition, boolean includeChildren) {
        String effectiveStatus = effectiveStatus(competition);
        List<CompetitionResponse.GroupResponse> groups = includeChildren
                ? competition.getGroups().stream()
                .filter(group -> !group.isDeleted())
                .map(group -> new CompetitionResponse.GroupResponse(
                        group.getId(), group.getName(), group.getEligibility()))
                .toList()
                : List.of();
        List<CompetitionResponse.RuleResponse> rules = includeChildren
                ? competition.getRules().stream()
                .filter(rule -> !rule.isDeleted())
                .map(rule -> toRuleResponse(rule, effectiveStatus))
                .toList()
                : List.of();
        return new CompetitionResponse(
                competition.getId(),
                competition.getName(),
                competition.getOrganizer(),
                competition.getCategory(),
                competition.getLevel(),
                competition.getOfficialWebsite(),
                competition.getRegisterStart(),
                competition.getRegisterEnd(),
                competition.getContestStart(),
                competition.getContestEnd(),
                competition.getDescription(),
                competition.getSourceUrl(),
                competition.getSourceDate(),
                effectiveStatus,
                competition.getCreatedAt(),
                competition.getUpdatedAt(),
                groups,
                rules);
    }

    private CompetitionResponse.RuleResponse toRuleResponse(CompetitionRule rule, String competitionStatus) {
        String status = "EXPIRED".equals(competitionStatus) ? "EXPIRED" : rule.getStatus();
        return new CompetitionResponse.RuleResponse(
                rule.getId(),
                rule.getGroup() == null ? null : rule.getGroup().getId(),
                rule.getGroup() == null ? null : rule.getGroup().getName(),
                rule.getTitle(),
                rule.getContent(),
                rule.getSourceUrl(),
                rule.getSourceDate(),
                rule.getPublishDate(),
                status);
    }

    private String effectiveStatus(Competition competition) {
        if ("EXPIRED".equalsIgnoreCase(competition.getStatus())) {
            return "EXPIRED";
        }
        if (competition.getContestEnd() != null && competition.getContestEnd().isBefore(LocalDate.now())) {
            return "EXPIRED";
        }
        return competition.getStatus();
    }
}

