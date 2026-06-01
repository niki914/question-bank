package com.example.questionbank.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.dto.*;
import com.example.questionbank.entity.*;
import com.example.questionbank.mapper.*;
import com.example.questionbank.service.ExamPaperService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamPaperServiceImpl implements ExamPaperService {

    private static final BigDecimal REQUIRED_COVERAGE = new BigDecimal("85");
    private static final double DIFFICULTY_TOLERANCE = 0.5;
    private static final Map<String, Double> DIFFICULTY_WEIGHT = Map.of(
            "EASY", 1.0, "MEDIUM", 2.0, "HARD", 3.0
    );

    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final QuestionKnowledgePointMapper qkpMapper;
    private final TeacherMapper teacherMapper;

    public ExamPaperServiceImpl(ExamPaperMapper paperMapper,
                                ExamPaperQuestionMapper paperQuestionMapper,
                                QuestionMapper questionMapper,
                                KnowledgePointMapper knowledgePointMapper,
                                QuestionKnowledgePointMapper qkpMapper,
                                TeacherMapper teacherMapper) {
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.qkpMapper = qkpMapper;
        this.teacherMapper = teacherMapper;
    }

    @Override
    @Transactional
    public PaperVO create(PaperCreateDTO dto) {
        // 1. 校验试卷数据
        PaperValidationResult validationResult = validateQuestions(dto.getQuestions(), dto.getTotalScore(), dto.getDifficulty());

        // 2. 插入试卷
        ExamPaper paper = new ExamPaper();
        paper.setTeacherId(dto.getTeacherId());
        paper.setPaperName(dto.getPaperName());
        paper.setTotalScore(dto.getTotalScore());
        paper.setDifficulty(dto.getDifficulty());
        paper.setQuestionTypeDistribution(dto.getQuestionTypeDistribution());
        paper.setActualTotalScore(validationResult.getActualTotalScore());
        paper.setKnowledgePointCoverage(validationResult.getKnowledgePointCoverage());
        paper.setValidationResult(JSON.toJSONString(validationResult));
        paper.setStatus("DRAFT");
        paperMapper.insert(paper);

        // 3. 批量插入试卷题目
        insertPaperQuestions(paper.getId(), dto.getQuestions());

        // 4. 返回结果
        return getById(paper.getId());
    }

    @Override
    @Transactional
    public PaperVO update(Long id, PaperCreateDTO dto) {
        ExamPaper existing = paperMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("试卷不存在");
        }
        if ("SUBMITTED".equals(existing.getStatus())) {
            throw new IllegalArgumentException("已提交的试卷不可编辑");
        }

        // 1. 删除旧题目
        paperQuestionMapper.delete(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, id));

        // 2. 校验并更新
        PaperValidationResult validationResult = validateQuestions(dto.getQuestions(), dto.getTotalScore(), dto.getDifficulty());
        existing.setPaperName(dto.getPaperName());
        existing.setTotalScore(dto.getTotalScore());
        existing.setDifficulty(dto.getDifficulty());
        existing.setQuestionTypeDistribution(dto.getQuestionTypeDistribution());
        existing.setActualTotalScore(validationResult.getActualTotalScore());
        existing.setKnowledgePointCoverage(validationResult.getKnowledgePointCoverage());
        existing.setValidationResult(JSON.toJSONString(validationResult));
        paperMapper.updateById(existing);

        insertPaperQuestions(id, dto.getQuestions());
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ExamPaper existing = paperMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("试卷不存在");
        }
        paperMapper.deleteById(id);
    }

    @Override
    public PaperVO getById(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new IllegalArgumentException("试卷不存在");
        }
        return buildPaperVO(paper);
    }

    @Override
    public Page<PaperVO> page(Long teacherId, Integer pageNum, Integer pageSize, String status) {
        Page<ExamPaper> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getTeacherId, teacherId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ExamPaper::getStatus, status);
        }
        wrapper.orderByDesc(ExamPaper::getCreateTime);
        Page<ExamPaper> result = paperMapper.selectPage(page, wrapper);

        Page<PaperVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<PaperVO> voList = result.getRecords().stream()
                .map(p -> {
                    PaperVO vo = new PaperVO();
                    vo.setId(p.getId());
                    vo.setTeacherId(p.getTeacherId());
                    vo.setPaperName(p.getPaperName());
                    vo.setTotalScore(p.getTotalScore());
                    vo.setDifficulty(p.getDifficulty());
                    vo.setStatus(p.getStatus());
                    vo.setQuestionTypeDistribution(p.getQuestionTypeDistribution());
                    vo.setKnowledgePointCoverage(p.getKnowledgePointCoverage());
                    vo.setActualTotalScore(p.getActualTotalScore());
                    vo.setValidationResult(p.getValidationResult());
                    vo.setCreateTime(p.getCreateTime());
                    vo.setUpdateTime(p.getUpdateTime());
                    Teacher teacher = teacherMapper.selectById(p.getTeacherId());
                    if (teacher != null) {
                        vo.setTeacherName(teacher.getName());
                    }
                    return vo;
                })
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public PaperValidationResult validate(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new IllegalArgumentException("试卷不存在");
        }

        List<ExamPaperQuestion> pqList = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, id)
                        .orderByAsc(ExamPaperQuestion::getQuestionOrder));

        List<PaperQuestionItem> items = pqList.stream()
                .map(pq -> {
                    PaperQuestionItem item = new PaperQuestionItem();
                    item.setQuestionId(pq.getQuestionId());
                    item.setQuestionScore(pq.getQuestionScore());
                    return item;
                })
                .collect(Collectors.toList());

        return validateQuestions(items, paper.getTotalScore(), paper.getDifficulty());
    }

    @Override
    @Transactional
    public PaperVO submit(Long id) {
        ExamPaper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new IllegalArgumentException("试卷不存在");
        }

        PaperValidationResult result = validate(id);
        paper.setStatus("SUBMITTED");
        paper.setKnowledgePointCoverage(result.getKnowledgePointCoverage());
        paper.setActualTotalScore(result.getActualTotalScore());
        paper.setValidationResult(JSON.toJSONString(result));
        paperMapper.updateById(paper);

        return getById(id);
    }

    // ==================== 核心校验逻辑 ====================

    private PaperValidationResult validateQuestions(List<PaperQuestionItem> items,
                                                     BigDecimal expectedTotal,
                                                     String targetDifficulty) {
        PaperValidationResult result = new PaperValidationResult();
        result.setRequiredCoverage(REQUIRED_COVERAGE);

        // --- 知识点覆盖率 ---
        List<KnowledgePoint> allKPs = knowledgePointMapper.selectList(null);
        int totalKPCount = allKPs.size();
        Map<Long, String> kpIdToName = allKPs.stream()
                .collect(Collectors.toMap(KnowledgePoint::getId, KnowledgePoint::getName, (a, b) -> a));
        Set<Long> allKPIds = new HashSet<>(kpIdToName.keySet());

        Set<Long> coveredKPIds = new HashSet<>();
        // 批量查询知识点关联
        List<Long> questionIds = items.stream().map(PaperQuestionItem::getQuestionId).collect(Collectors.toList());

        // 批量查询 question_knowledge_point
        List<QuestionKnowledgePoint> allQkps = qkpMapper.selectList(
                new LambdaQueryWrapper<QuestionKnowledgePoint>().in(QuestionKnowledgePoint::getQuestionId, questionIds));
        Set<Long> kpIdsFromTable = allQkps.stream()
                .map(QuestionKnowledgePoint::getKnowledgePointId)
                .collect(Collectors.toSet());

        if (!kpIdsFromTable.isEmpty()) {
            coveredKPIds.addAll(kpIdsFromTable);
        } else {
            // 降级：用 question.knowledge_points 逗号分隔字符串
            List<Question> questions = questionMapper.selectBatchIds(questionIds);
            Set<String> allKPNames = allKPs.stream().map(KnowledgePoint::getName).collect(Collectors.toSet());
            Map<String, Long> nameToId = allKPs.stream()
                    .collect(Collectors.toMap(KnowledgePoint::getName, KnowledgePoint::getId, (a, b) -> a));
            for (Question q : questions) {
                if (q.getKnowledgePoints() != null && !q.getKnowledgePoints().isEmpty()) {
                    String[] names = q.getKnowledgePoints().split(",");
                    for (String name : names) {
                        String trimmed = name.trim();
                        if (nameToId.containsKey(trimmed)) {
                            coveredKPIds.add(nameToId.get(trimmed));
                        }
                    }
                }
            }
        }

        BigDecimal coverage = totalKPCount == 0 ? BigDecimal.ZERO
                : new BigDecimal(coveredKPIds.size())
                .multiply(new BigDecimal(100))
                .divide(new BigDecimal(totalKPCount), 2, RoundingMode.HALF_UP);
        result.setKnowledgePointCoverage(coverage);

        List<String> uncoveredIds = allKPIds.stream()
                .filter(id -> !coveredKPIds.contains(id))
                .map(kpIdToName::get)
                .collect(Collectors.toList());
        result.setUncoveredKnowledgePoints(uncoveredIds);

        // --- 难度匹配 ---
        Map<String, Long> diffCount = new HashMap<>();
        for (Long qId : questionIds) {
            Question q = questionMapper.selectById(qId);
            if (q != null && q.getDifficulty() != null) {
                diffCount.merge(q.getDifficulty(), 1L, Long::sum);
            }
        }
        double weightedSum = diffCount.getOrDefault("EASY", 0L) * 1.0
                           + diffCount.getOrDefault("MEDIUM", 0L) * 2.0
                           + diffCount.getOrDefault("HARD", 0L) * 3.0;
        double avgDiff = questionIds.isEmpty() ? 0 : weightedSum / questionIds.size();
        double targetVal = DIFFICULTY_WEIGHT.getOrDefault(targetDifficulty, 2.0);
        boolean matched = Math.abs(avgDiff - targetVal) <= DIFFICULTY_TOLERANCE;
        result.setDifficultyMatched(matched);
        result.setDifficultyDetail(String.format("平均难度值: %.2f, 目标: %.1f, 允许偏差: %.1f", avgDiff, targetVal, DIFFICULTY_TOLERANCE));

        // --- 总分校验 ---
        BigDecimal actualTotal = items.stream()
                .map(PaperQuestionItem::getQuestionScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setActualTotalScore(actualTotal);
        result.setExpectedTotalScore(expectedTotal);
        result.setTotalScoreMatched(actualTotal.compareTo(expectedTotal) == 0);

        // --- 最终判定 ---
        result.setPassed(coverage.compareTo(REQUIRED_COVERAGE) >= 0
                      && result.isDifficultyMatched()
                      && result.isTotalScoreMatched());

        return result;
    }

    // ==================== 辅助方法 ====================

    private void insertPaperQuestions(Long paperId, List<PaperQuestionItem> items) {
        List<ExamPaperQuestion> entities = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            PaperQuestionItem item = items.get(i);
            ExamPaperQuestion entity = new ExamPaperQuestion();
            entity.setPaperId(paperId);
            entity.setQuestionId(item.getQuestionId());
            entity.setQuestionScore(item.getQuestionScore());
            entity.setQuestionOrder(i + 1);
            entities.add(entity);
        }
        for (ExamPaperQuestion entity : entities) {
            paperQuestionMapper.insert(entity);
        }
    }

    private PaperVO buildPaperVO(ExamPaper paper) {
        PaperVO vo = new PaperVO();
        vo.setId(paper.getId());
        vo.setTeacherId(paper.getTeacherId());
        vo.setPaperName(paper.getPaperName());
        vo.setTotalScore(paper.getTotalScore());
        vo.setDifficulty(paper.getDifficulty());
        vo.setStatus(paper.getStatus());
        vo.setQuestionTypeDistribution(paper.getQuestionTypeDistribution());
        vo.setKnowledgePointCoverage(paper.getKnowledgePointCoverage());
        vo.setActualTotalScore(paper.getActualTotalScore());
        vo.setValidationResult(paper.getValidationResult());
        vo.setCreateTime(paper.getCreateTime());
        vo.setUpdateTime(paper.getUpdateTime());

        Teacher teacher = teacherMapper.selectById(paper.getTeacherId());
        if (teacher != null) {
            vo.setTeacherName(teacher.getName());
        }

        // 查询题目列表
        List<ExamPaperQuestion> pqList = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, paper.getId())
                        .orderByAsc(ExamPaperQuestion::getQuestionOrder));

        List<PaperQuestionDetail> details = new ArrayList<>();
        if (!pqList.isEmpty()) {
            List<Long> qIds = pqList.stream().map(ExamPaperQuestion::getQuestionId).collect(Collectors.toList());
            List<Question> questions = questionMapper.selectBatchIds(qIds);
            Map<Long, Question> qMap = questions.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

            for (ExamPaperQuestion pq : pqList) {
                PaperQuestionDetail detail = new PaperQuestionDetail();
                detail.setId(pq.getId());
                detail.setQuestionId(pq.getQuestionId());
                detail.setQuestionScore(pq.getQuestionScore());
                detail.setQuestionOrder(pq.getQuestionOrder());
                Question q = qMap.get(pq.getQuestionId());
                if (q != null) {
                    detail.setQuestionTitle(q.getTitle());
                    detail.setQuestionType(q.getType());
                    detail.setQuestionDifficulty(q.getDifficulty());
                    detail.setQuestionChapter(q.getChapter());
                    detail.setKnowledgePoints(q.getKnowledgePoints());
                    detail.setContentJson(q.getContentJson());
                }
                details.add(detail);
            }
        }
        vo.setQuestions(details);
        return vo;
    }
}
