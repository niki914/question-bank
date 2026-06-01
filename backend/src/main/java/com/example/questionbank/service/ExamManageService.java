package com.example.questionbank.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.dto.*;
import java.util.List;

public interface ExamManageService {
    ExamVO createExam(ExamCreateDTO dto);
    ExamVO updateExam(Long id, ExamCreateDTO dto);
    void deleteExam(Long id);
    ExamVO getExamById(Long id);
    Page<ExamVO> pageExams(Long teacherId, Integer pageNum, Integer pageSize, String status);

    /** 开始考试（PENDING -> ONGOING） */
    ExamVO startExam(Long id);
    /** 结束考试（ONGOING -> GRADING） */
    ExamVO endExam(Long id);
    /** 完成评分（GRADING -> FINISHED） */
    ExamVO finishGrading(Long id);

    /** 考生提交答卷 */
    StudentResultVO submitAnswer(StudentSubmitDTO dto);
    /** 获取考生成绩汇总列表 */
    List<StudentResultVO> listResults(Long examId);
    /** 获取某考生详细答题记录 */
    StudentResultVO getStudentResult(Long examId, String studentNo);
    /** 教师对主观题评分 */
    StudentAnswerVO gradeSubjective(TeacherGradeDTO dto);
    /** 汇总某考生总分（所有题目评完后调用） */
    StudentResultVO calcTotalScore(Long examId, String studentNo);
}
