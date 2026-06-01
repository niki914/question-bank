package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.questionbank.dto.AutoGeneratePaperRequest;
import com.example.questionbank.dto.AutoGeneratePaperResponse;
import com.example.questionbank.dto.PaperQuestionVO;
import com.example.questionbank.dto.PaperTypeRuleDTO;
import com.example.questionbank.entity.CourseChapter;
import com.example.questionbank.entity.KnowledgePoint;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.QuestionKnowledgePoint;
import com.example.questionbank.enums.Difficulty;
import com.example.questionbank.enums.QuestionType;
import com.example.questionbank.mapper.CourseChapterMapper;
import com.example.questionbank.mapper.KnowledgePointMapper;
import com.example.questionbank.mapper.QuestionKnowledgePointMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.service.PaperService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PaperServiceImpl implements PaperService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final CourseChapterMapper courseChapterMapper;
    private final QuestionMapper questionMapper;
    private final QuestionKnowledgePointMapper questionKnowledgePointMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    public PaperServiceImpl(CourseChapterMapper courseChapterMapper,
                            QuestionMapper questionMapper,
                            QuestionKnowledgePointMapper questionKnowledgePointMapper,
                            KnowledgePointMapper knowledgePointMapper) {
        this.courseChapterMapper = courseChapterMapper;
        this.questionMapper = questionMapper;
        this.questionKnowledgePointMapper = questionKnowledgePointMapper;
        this.knowledgePointMapper = knowledgePointMapper;
    }

    @Override
    public AutoGeneratePaperResponse autoGenerate(AutoGeneratePaperRequest request) {
        if (request == null) {
            return buildFailureResponse("请求参数不能为空");
        }
        if (request.getTeacherId() == null) {
            return buildFailureResponse("教师ID不能为空");
        }
        if (request.getTotalScore() == null || request.getTotalScore() <= 0) {
            return buildFailureResponse("总分必须大于0");
        }
        if (!hasText(request.getDifficulty())) {
            return buildFailureResponse("难度不能为空");
        }
        if (!isValidDifficulty(request.getDifficulty())) {
            return buildFailureResponse("难度参数非法");
        }
        if (request.getTypeRules() == null || request.getTypeRules().isEmpty()) {
            return buildFailureResponse("请至少选择一种题型");
        }

        int totalQuestionCount = 0;
        List<Question> selectedQuestions = new ArrayList<>();
        Set<Long> selectedQuestionIds = new HashSet<>();
        Set<Long> scopedQuestionIds = new LinkedHashSet<>();
        Set<Long> scopedChapterIds = collectScopedChapterIds(request.getChapterId());
        Set<String> scopedChapterNames = collectScopedChapterNames(scopedChapterIds);

        for (PaperTypeRuleDTO rule : request.getTypeRules()) {
            if (rule == null || !hasText(rule.getType())) {
                return buildFailureResponse("题型不能为空");
            }
            if (!isValidQuestionType(rule.getType())) {
                return buildFailureResponse("题型参数非法");
            }
            if (rule.getCount() == null || rule.getCount() <= 0) {
                return buildFailureResponse("题目数量必须大于0");
            }

            totalQuestionCount += rule.getCount();
            List<Question> candidates = listCandidates(
                    request.getTeacherId(),
                    request.getDifficulty(),
                    request.getChapter(),
                    scopedChapterNames,
                    rule.getType()
            );
            for (Question candidate : candidates) {
                scopedQuestionIds.add(candidate.getId());
            }
            if (candidates.size() < rule.getCount()) {
                return buildFailureResponse("题型 " + rule.getType() + " 可用题目不足");
            }

            List<Question> pickedQuestions = pickQuestionsByRule(candidates, rule.getCount(), selectedQuestionIds);
            if (pickedQuestions.size() < rule.getCount()) {
                return buildFailureResponse("题型 " + rule.getType() + " 可用题目不足");
            }
            selectedQuestions.addAll(pickedQuestions);
        }

        if (totalQuestionCount <= 0) {
            return buildFailureResponse("题目数量必须大于0");
        }

        BigDecimal averageScore = calculateAverageScore(request.getTotalScore(), totalQuestionCount);
        List<PaperQuestionVO> questions = buildQuestionVOList(selectedQuestions, averageScore, request.getTotalScore());

        Map<Long, Set<Long>> selectedKnowledgePointMap = buildQuestionKnowledgePointMap(
                selectedQuestions.stream().map(Question::getId).collect(Collectors.toList())
        );
        Map<Long, Set<Long>> scopedKnowledgePointMap = buildQuestionKnowledgePointMap(new ArrayList<>(scopedQuestionIds));

        Set<Long> coveredKnowledgePointIds = selectedKnowledgePointMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> scopedKnowledgePointIds = scopedKnowledgePointMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, String> knowledgePointNameMap = buildKnowledgePointNameMap(scopedKnowledgePointIds);
        Set<Long> uncoveredKnowledgePointIds = new LinkedHashSet<>(scopedKnowledgePointIds);
        uncoveredKnowledgePointIds.removeAll(coveredKnowledgePointIds);

        AutoGeneratePaperResponse response = new AutoGeneratePaperResponse();
        response.setSuccess(true);
        response.setMessage("组卷成功");
        response.setTotalScore(request.getTotalScore());
        response.setTotalQuestionCount(totalQuestionCount);
        response.setQuestionScore(averageScore);
        response.setActualTotalScore(questions.stream()
                .map(PaperQuestionVO::getScore)
                .filter(Objects::nonNull)
                .reduce(ZERO, BigDecimal::add));
        response.setDifficulty(request.getDifficulty());
        response.setTotalKnowledgePointCount(scopedKnowledgePointIds.size());
        response.setCoveredKnowledgePointCount(coveredKnowledgePointIds.size());
        response.setCoverageRate(calculateCoverageRate(coveredKnowledgePointIds.size(), scopedKnowledgePointIds.size()));
        response.setUncoveredKnowledgePoints(uncoveredKnowledgePointIds.stream()
                .map(knowledgePointNameMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        response.setQuestions(questions);
        return response;
    }

    private List<Question> listCandidates(Long teacherId,
                                          String difficulty,
                                          String chapter,
                                          Set<String> scopedChapterNames,
                                          String type) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Question::getTeacherId, teacherId)
                .eq(Question::getDifficulty, difficulty)
                .eq(Question::getType, type)
                .orderByDesc(Question::getUpdateTime)
                .orderByDesc(Question::getId);
        if (scopedChapterNames != null && !scopedChapterNames.isEmpty()) {
            wrapper.in(Question::getChapter, scopedChapterNames);
        } else if (hasText(chapter)) {
            wrapper.eq(Question::getChapter, chapter.trim());
        }
        return questionMapper.selectList(wrapper);
    }

    private List<Question> pickQuestionsByRule(List<Question> candidates, Integer count, Set<Long> selectedQuestionIds) {
        List<Question> pickedQuestions = new ArrayList<>();
        for (Question candidate : candidates) {
            if (selectedQuestionIds.contains(candidate.getId())) {
                continue;
            }
            selectedQuestionIds.add(candidate.getId());
            pickedQuestions.add(candidate);
            if (pickedQuestions.size() >= count) {
                break;
            }
        }
        return pickedQuestions;
    }

    private Map<Long, Set<Long>> buildQuestionKnowledgePointMap(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionKnowledgePoint> relations = questionKnowledgePointMapper.selectList(
                new LambdaQueryWrapper<QuestionKnowledgePoint>()
                        .in(QuestionKnowledgePoint::getQuestionId, questionIds)
        );
        Map<Long, Set<Long>> result = new HashMap<>();
        for (QuestionKnowledgePoint relation : relations) {
            result.computeIfAbsent(relation.getQuestionId(), key -> new LinkedHashSet<>())
                    .add(relation.getKnowledgePointId());
        }
        return result;
    }

    private Map<Long, String> buildKnowledgePointNameMap(Set<Long> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return knowledgePointMapper.selectBatchIds(knowledgePointIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName, (left, right) -> left));
    }

    private BigDecimal calculateAverageScore(Integer totalScore, Integer totalQuestionCount) {
        return BigDecimal.valueOf(totalScore)
                .divide(BigDecimal.valueOf(totalQuestionCount), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCoverageRate(int coveredKnowledgePointCount, int totalKnowledgePointCount) {
        if (totalKnowledgePointCount <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(coveredKnowledgePointCount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(totalKnowledgePointCount), 2, RoundingMode.HALF_UP);
    }

    private List<PaperQuestionVO> buildQuestionVOList(List<Question> questions, BigDecimal averageScore, Integer totalScore) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }

        List<PaperQuestionVO> result = new ArrayList<>();
        BigDecimal remainingScore = BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP);
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            BigDecimal questionScore = i == questions.size() - 1 ? remainingScore : averageScore;
            remainingScore = remainingScore.subtract(questionScore);

            PaperQuestionVO vo = new PaperQuestionVO();
            vo.setId(question.getId());
            vo.setType(question.getType());
            vo.setTitle(question.getTitle());
            vo.setDifficulty(question.getDifficulty());
            vo.setChapter(question.getChapter());
            vo.setKnowledgePoints(question.getKnowledgePoints());
            vo.setScore(questionScore);
            result.add(vo);
        }
        return result;
    }

    private AutoGeneratePaperResponse buildFailureResponse(String message) {
        AutoGeneratePaperResponse response = new AutoGeneratePaperResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTotalQuestionCount(0);
        response.setQuestionScore(ZERO);
        response.setActualTotalScore(ZERO);
        response.setTotalKnowledgePointCount(0);
        response.setCoveredKnowledgePointCount(0);
        response.setCoverageRate(ZERO);
        response.setUncoveredKnowledgePoints(Collections.emptyList());
        response.setQuestions(Collections.emptyList());
        return response;
    }

    private Set<Long> collectScopedChapterIds(Long chapterId) {
        if (chapterId == null) {
            return Collections.emptySet();
        }
        List<CourseChapter> chapters = courseChapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                .orderByAsc(CourseChapter::getSortOrder)
                .orderByAsc(CourseChapter::getId));
        if (chapters == null || chapters.isEmpty()) {
            return Collections.emptySet();
        }

        Map<Long, List<CourseChapter>> childrenMap = new HashMap<>();
        for (CourseChapter chapter : chapters) {
            Long parentId = chapter.getParentId() == null ? 0L : chapter.getParentId();
            childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(chapter);
        }

        Set<Long> result = new LinkedHashSet<>();
        collectChildChapterIds(chapterId, childrenMap, result);
        return result;
    }

    private void collectChildChapterIds(Long chapterId,
                                        Map<Long, List<CourseChapter>> childrenMap,
                                        Set<Long> result) {
        if (chapterId == null || result.contains(chapterId)) {
            return;
        }
        result.add(chapterId);
        for (CourseChapter child : childrenMap.getOrDefault(chapterId, Collections.emptyList())) {
            collectChildChapterIds(child.getId(), childrenMap, result);
        }
    }

    private Set<String> collectScopedChapterNames(Set<Long> scopedChapterIds) {
        if (scopedChapterIds == null || scopedChapterIds.isEmpty()) {
            return Collections.emptySet();
        }
        return courseChapterMapper.selectBatchIds(scopedChapterIds).stream()
                .filter(Objects::nonNull)
                .map(CourseChapter::getName)
                .filter(this::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isValidDifficulty(String difficulty) {
        for (Difficulty value : Difficulty.values()) {
            if (value.name().equals(difficulty)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidQuestionType(String type) {
        for (QuestionType value : QuestionType.values()) {
            if (value.name().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
