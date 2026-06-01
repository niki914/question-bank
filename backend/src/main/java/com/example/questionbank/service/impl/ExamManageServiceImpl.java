package com.example.questionbank.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.dto.*;
import com.example.questionbank.entity.*;
import com.example.questionbank.mapper.*;
import com.example.questionbank.service.ExamManageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamManageServiceImpl implements ExamManageService {

    private final ExamMapper examMapper;
    private final ExamRoomMapper roomMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final TeacherMapper teacherMapper;
    private final ExamStudentAnswerMapper answerMapper;
    private final ExamStudentResultMapper resultMapper;

    public ExamManageServiceImpl(ExamMapper examMapper,
                                 ExamRoomMapper roomMapper,
                                 ExamPaperMapper paperMapper,
                                 ExamPaperQuestionMapper paperQuestionMapper,
                                 QuestionMapper questionMapper,
                                 TeacherMapper teacherMapper,
                                 ExamStudentAnswerMapper answerMapper,
                                 ExamStudentResultMapper resultMapper) {
        this.examMapper = examMapper;
        this.roomMapper = roomMapper;
        this.paperMapper = paperMapper;
        this.paperQuestionMapper = paperQuestionMapper;
        this.questionMapper = questionMapper;
        this.teacherMapper = teacherMapper;
        this.answerMapper = answerMapper;
        this.resultMapper = resultMapper;
    }

    // ==================== 考试 CRUD ====================

    @Override
    @Transactional
    public ExamVO createExam(ExamCreateDTO dto) {
        // 试卷必须已提交
        ExamPaper paper = paperMapper.selectById(dto.getPaperId());
        if (paper == null) throw new IllegalArgumentException("试卷不存在");
        if (!"SUBMITTED".equals(paper.getStatus())) {
            throw new IllegalArgumentException("只能使用已提交的试卷");
        }
        ExamRoom room = roomMapper.selectById(dto.getRoomId());
        if (room == null) throw new IllegalArgumentException("考场不存在");

        Exam exam = new Exam();
        copyDto(dto, exam);
        exam.setStatus("PENDING");
        examMapper.insert(exam);
        return buildExamVO(exam);
    }

    @Override
    @Transactional
    public ExamVO updateExam(Long id, ExamCreateDTO dto) {
        Exam exam = requireExam(id);
        if (!"PENDING".equals(exam.getStatus())) {
            throw new IllegalArgumentException("只有待开始的考试可以编辑");
        }
        ExamPaper paper = paperMapper.selectById(dto.getPaperId());
        if (paper == null || !"SUBMITTED".equals(paper.getStatus())) {
            throw new IllegalArgumentException("只能使用已提交的试卷");
        }
        copyDto(dto, exam);
        examMapper.updateById(exam);
        return buildExamVO(exam);
    }

    @Override
    @Transactional
    public void deleteExam(Long id) {
        Exam exam = requireExam(id);
        if ("ONGOING".equals(exam.getStatus())) {
            throw new IllegalArgumentException("考试进行中，不可删除");
        }
        examMapper.deleteById(id);
    }

    @Override
    public ExamVO getExamById(Long id) {
        return buildExamVO(requireExam(id));
    }

    @Override
    public Page<ExamVO> pageExams(Long teacherId, Integer pageNum, Integer pageSize, String status) {
        Page<Exam> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .eq(Exam::getTeacherId, teacherId)
                .orderByDesc(Exam::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Exam::getStatus, status);
        }
        Page<Exam> result = examMapper.selectPage(page, wrapper);
        Page<ExamVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::buildExamVO).collect(Collectors.toList()));
        return voPage;
    }

    // ==================== 考试状态流转 ====================

    @Override
    @Transactional
    public ExamVO startExam(Long id) {
        Exam exam = requireExam(id);
        if (!"PENDING".equals(exam.getStatus())) {
            throw new IllegalArgumentException("只有待开始状态的考试可以开始");
        }
        exam.setStatus("ONGOING");
        examMapper.updateById(exam);
        return buildExamVO(exam);
    }

    @Override
    @Transactional
    public ExamVO endExam(Long id) {
        Exam exam = requireExam(id);
        if (!"ONGOING".equals(exam.getStatus())) {
            throw new IllegalArgumentException("只有进行中的考试可以结束");
        }
        exam.setStatus("GRADING");
        examMapper.updateById(exam);
        return buildExamVO(exam);
    }

    @Override
    @Transactional
    public ExamVO finishGrading(Long id) {
        Exam exam = requireExam(id);
        if (!"GRADING".equals(exam.getStatus())) {
            throw new IllegalArgumentException("只有评分中状态的考试可以完成评分");
        }
        exam.setStatus("FINISHED");
        examMapper.updateById(exam);
        return buildExamVO(exam);
    }

    // ==================== 考生作答 ====================

    @Override
    @Transactional
    public StudentResultVO submitAnswer(StudentSubmitDTO dto) {
        Exam exam = requireExam(dto.getExamId());
        if (!"ONGOING".equals(exam.getStatus())) {
            throw new IllegalArgumentException("当前考试不在进行中，无法提交答案");
        }

        // 删除已有记录（重复提交覆盖）
        answerMapper.delete(new LambdaQueryWrapper<ExamStudentAnswer>()
                .eq(ExamStudentAnswer::getExamId, dto.getExamId())
                .eq(ExamStudentAnswer::getStudentNo, dto.getStudentNo()));

        // 获取试卷题目，建立 questionId -> pq 映射
        List<ExamPaperQuestion> pqList = paperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, exam.getPaperId()));
        Map<Long, ExamPaperQuestion> pqMap = pqList.stream()
                .collect(Collectors.toMap(ExamPaperQuestion::getQuestionId, p -> p, (a, b) -> a));

        List<ExamStudentAnswer> answers = new ArrayList<>();
        for (StudentSubmitDTO.AnswerItem item : dto.getAnswers()) {
            Question q = questionMapper.selectById(item.getQuestionId());
            if (q == null) continue;

            ExamStudentAnswer ans = new ExamStudentAnswer();
            ans.setExamId(dto.getExamId());
            ans.setStudentName(dto.getStudentName());
            ans.setStudentNo(dto.getStudentNo());
            ans.setQuestionId(item.getQuestionId());
            ans.setAnswerContent(item.getAnswerContent());
            ans.setAnswerImagePath(item.getAnswerImagePath());

            // 客观题自动判分
            ExamPaperQuestion pq = pqMap.get(item.getQuestionId());
            BigDecimal score = pq != null ? pq.getQuestionScore() : BigDecimal.ZERO;

            String type = q.getType();
            if ("SINGLE_CHOICE".equals(type) || "MULTIPLE_CHOICE".equals(type)) {
                BigDecimal autoScore = autoGradeChoice(q, item.getAnswerContent(), score);
                ans.setAutoScore(autoScore);
                ans.setFinalScore(autoScore);
            } else if ("FILL_BLANK".equals(type)) {
                BigDecimal autoScore = autoGradeFillBlank(q, item.getAnswerContent(), score);
                ans.setAutoScore(autoScore);
                ans.setFinalScore(autoScore);
            } else {
                // 主观题：等待教师评分
                ans.setAutoScore(BigDecimal.ZERO);
                ans.setFinalScore(null);
            }
            answers.add(ans);
        }
        for (ExamStudentAnswer a : answers) {
            answerMapper.insert(a);
        }

        // 更新或插入成绩汇总行
        ExamStudentResult result = resultMapper.selectOne(
                new LambdaQueryWrapper<ExamStudentResult>()
                        .eq(ExamStudentResult::getExamId, dto.getExamId())
                        .eq(ExamStudentResult::getStudentNo, dto.getStudentNo()));
        if (result == null) {
            result = new ExamStudentResult();
            result.setExamId(dto.getExamId());
            result.setStudentName(dto.getStudentName());
            result.setStudentNo(dto.getStudentNo());
            result.setStatus("SUBMITTED");
            result.setTotalScore(BigDecimal.ZERO);
            resultMapper.insert(result);
        } else {
            result.setStatus("SUBMITTED");
            result.setTotalScore(BigDecimal.ZERO);
            resultMapper.updateById(result);
        }

        return buildStudentResultVO(result, answers, pqMap);
    }

    @Override
    public List<StudentResultVO> listResults(Long examId) {
        List<ExamStudentResult> results = resultMapper.selectList(
                new LambdaQueryWrapper<ExamStudentResult>()
                        .eq(ExamStudentResult::getExamId, examId));
        return results.stream().map(r -> {
            List<ExamStudentAnswer> answers = answerMapper.selectList(
                    new LambdaQueryWrapper<ExamStudentAnswer>()
                            .eq(ExamStudentAnswer::getExamId, examId)
                            .eq(ExamStudentAnswer::getStudentNo, r.getStudentNo()));
            return buildStudentResultVO(r, answers, null);
        }).collect(Collectors.toList());
    }

    @Override
    public StudentResultVO getStudentResult(Long examId, String studentNo) {
        ExamStudentResult result = resultMapper.selectOne(
                new LambdaQueryWrapper<ExamStudentResult>()
                        .eq(ExamStudentResult::getExamId, examId)
                        .eq(ExamStudentResult::getStudentNo, studentNo));
        if (result == null) throw new IllegalArgumentException("未找到该考生成绩");
        List<ExamStudentAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamStudentAnswer>()
                        .eq(ExamStudentAnswer::getExamId, examId)
                        .eq(ExamStudentAnswer::getStudentNo, studentNo));
        return buildStudentResultVO(result, answers, null);
    }

    @Override
    @Transactional
    public StudentAnswerVO gradeSubjective(TeacherGradeDTO dto) {
        ExamStudentAnswer ans = answerMapper.selectById(dto.getAnswerId());
        if (ans == null) throw new IllegalArgumentException("答题记录不存在");
        Question q = questionMapper.selectById(ans.getQuestionId());
        if (q == null || !"SUBJECTIVE".equals(q.getType())) {
            throw new IllegalArgumentException("该题不是主观题");
        }
        ans.setTeacherScore(dto.getScore());
        ans.setFinalScore(dto.getScore());
        ans.setGradedBy(dto.getGradedBy());
        ans.setGradedTime(LocalDateTime.now());
        answerMapper.updateById(ans);
        return buildAnswerVO(ans, q, null);
    }

    @Override
    @Transactional
    public StudentResultVO calcTotalScore(Long examId, String studentNo) {
        ExamStudentResult result = resultMapper.selectOne(
                new LambdaQueryWrapper<ExamStudentResult>()
                        .eq(ExamStudentResult::getExamId, examId)
                        .eq(ExamStudentResult::getStudentNo, studentNo));
        if (result == null) throw new IllegalArgumentException("未找到该考生成绩");

        List<ExamStudentAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamStudentAnswer>()
                        .eq(ExamStudentAnswer::getExamId, examId)
                        .eq(ExamStudentAnswer::getStudentNo, studentNo));

        BigDecimal total = answers.stream()
                .map(a -> a.getFinalScore() != null ? a.getFinalScore() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.setTotalScore(total);
        result.setStatus("GRADED");
        resultMapper.updateById(result);
        return buildStudentResultVO(result, answers, null);
    }

    // ==================== 自动判分 ====================

    private BigDecimal autoGradeChoice(Question q, String answerContent, BigDecimal fullScore) {
        if (answerContent == null || answerContent.isBlank()) return BigDecimal.ZERO;
        try {
            JSONObject content = JSON.parseObject(q.getContentJson());
            JSONArray options = content.getJSONArray("options");
            // 收集正确答案的 label 集合
            Set<String> correct = new HashSet<>();
            for (int i = 0; i < options.size(); i++) {
                JSONObject opt = options.getJSONObject(i);
                if (Boolean.TRUE.equals(opt.getBoolean("isCorrect"))) {
                    correct.add(opt.getString("label").trim().toUpperCase());
                }
            }
            // 学生答案
            Set<String> student = Arrays.stream(answerContent.split(","))
                    .map(s -> s.trim().toUpperCase())
                    .collect(Collectors.toSet());
            return correct.equals(student) ? fullScore : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal autoGradeFillBlank(Question q, String answerContent, BigDecimal fullScore) {
        if (answerContent == null || answerContent.isBlank()) return BigDecimal.ZERO;
        try {
            JSONObject content = JSON.parseObject(q.getContentJson());
            JSONArray blanks = content.getJSONArray("blanks");
            String[] studentAnswers = answerContent.split(",", -1);
            int totalBlanks = blanks.size();
            int correctCount = 0;
            for (int i = 0; i < totalBlanks; i++) {
                String correct = blanks.getJSONObject(i).getString("answer").trim();
                String student = i < studentAnswers.length ? studentAnswers[i].trim() : "";
                if (correct.equalsIgnoreCase(student)) correctCount++;
            }
            if (totalBlanks == 0) return BigDecimal.ZERO;
            // 按空数比例给分
            return fullScore.multiply(new BigDecimal(correctCount))
                    .divide(new BigDecimal(totalBlanks), 2, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ==================== VO 构建 ====================

    private ExamVO buildExamVO(Exam exam) {
        ExamVO vo = new ExamVO();
        vo.setId(exam.getId());
        vo.setExamName(exam.getExamName());
        vo.setPaperId(exam.getPaperId());
        vo.setRoomId(exam.getRoomId());
        vo.setTeacherId(exam.getTeacherId());
        vo.setStartTime(exam.getStartTime());
        vo.setDurationMinutes(exam.getDurationMinutes());
        vo.setStatus(exam.getStatus());
        vo.setCreateTime(exam.getCreateTime());
        vo.setUpdateTime(exam.getUpdateTime());

        ExamPaper paper = paperMapper.selectById(exam.getPaperId());
        if (paper != null) {
            vo.setPaperName(paper.getPaperName());
            // 附带试卷题目列表
            List<ExamPaperQuestion> pqList = paperQuestionMapper.selectList(
                    new LambdaQueryWrapper<ExamPaperQuestion>()
                            .eq(ExamPaperQuestion::getPaperId, paper.getId())
                            .orderByAsc(ExamPaperQuestion::getQuestionOrder));
            List<PaperQuestionDetail> details = new ArrayList<>();
            for (ExamPaperQuestion pq : pqList) {
                PaperQuestionDetail d = new PaperQuestionDetail();
                d.setId(pq.getId());
                d.setQuestionId(pq.getQuestionId());
                d.setQuestionScore(pq.getQuestionScore());
                d.setQuestionOrder(pq.getQuestionOrder());
                Question q = questionMapper.selectById(pq.getQuestionId());
                if (q != null) {
                    d.setQuestionTitle(q.getTitle());
                    d.setQuestionType(q.getType());
                    d.setQuestionDifficulty(q.getDifficulty());
                    d.setQuestionChapter(q.getChapter());
                    d.setKnowledgePoints(q.getKnowledgePoints());
                    d.setContentJson(q.getContentJson());
                }
                details.add(d);
            }
            vo.setQuestions(details);
        }
        ExamRoom room = roomMapper.selectById(exam.getRoomId());
        if (room != null) vo.setRoomName(room.getRoomName());
        Teacher teacher = teacherMapper.selectById(exam.getTeacherId());
        if (teacher != null) vo.setTeacherName(teacher.getName());
        return vo;
    }

    private StudentResultVO buildStudentResultVO(ExamStudentResult result,
                                                  List<ExamStudentAnswer> answers,
                                                  Map<Long, ExamPaperQuestion> pqMap) {
        StudentResultVO vo = new StudentResultVO();
        vo.setId(result.getId());
        vo.setExamId(result.getExamId());
        vo.setStudentName(result.getStudentName());
        vo.setStudentNo(result.getStudentNo());
        vo.setTotalScore(result.getTotalScore());
        vo.setStatus(result.getStatus());
        vo.setSubmitTime(result.getSubmitTime());

        // 若 pqMap 为 null，重新查询
        Map<Long, ExamPaperQuestion> pq = pqMap;
        if (pq == null) {
            Exam exam = examMapper.selectById(result.getExamId());
            if (exam != null) {
                List<ExamPaperQuestion> list = paperQuestionMapper.selectList(
                        new LambdaQueryWrapper<ExamPaperQuestion>()
                                .eq(ExamPaperQuestion::getPaperId, exam.getPaperId()));
                pq = list.stream().collect(Collectors.toMap(ExamPaperQuestion::getQuestionId, p -> p, (a, b) -> a));
            } else {
                pq = Collections.emptyMap();
            }
        }

        List<StudentAnswerVO> ansVOs = new ArrayList<>();
        for (ExamStudentAnswer a : answers) {
            Question q = questionMapper.selectById(a.getQuestionId());
            ExamPaperQuestion p = pq.get(a.getQuestionId());
            ansVOs.add(buildAnswerVO(a, q, p));
        }
        vo.setAnswers(ansVOs);
        return vo;
    }

    private StudentAnswerVO buildAnswerVO(ExamStudentAnswer a, Question q, ExamPaperQuestion pq) {
        StudentAnswerVO vo = new StudentAnswerVO();
        vo.setId(a.getId());
        vo.setQuestionId(a.getQuestionId());
        vo.setAnswerContent(a.getAnswerContent());
        vo.setAnswerImagePath(a.getAnswerImagePath());
        vo.setAutoScore(a.getAutoScore());
        vo.setTeacherScore(a.getTeacherScore());
        vo.setFinalScore(a.getFinalScore());
        vo.setGradedBy(a.getGradedBy());
        vo.setGradedTime(a.getGradedTime());
        if (q != null) {
            vo.setQuestionTitle(q.getTitle());
            vo.setQuestionType(q.getType());
        }
        if (pq != null) {
            vo.setQuestionScore(pq.getQuestionScore());
        }
        return vo;
    }

    private Exam requireExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new IllegalArgumentException("考试不存在");
        return exam;
    }

    private void copyDto(ExamCreateDTO dto, Exam exam) {
        exam.setExamName(dto.getExamName());
        exam.setPaperId(dto.getPaperId());
        exam.setRoomId(dto.getRoomId());
        exam.setTeacherId(dto.getTeacherId());
        exam.setStartTime(dto.getStartTime());
        exam.setDurationMinutes(dto.getDurationMinutes());
    }
}
