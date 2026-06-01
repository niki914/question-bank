package com.example.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 考生提交答卷请求 —— 一次提交该考生全部题目作答
 */
@Data
public class StudentSubmitDTO {
    @NotNull(message = "考试ID不能为空")
    private Long examId;
    @NotBlank(message = "姓名不能为空")
    private String studentName;
    @NotBlank(message = "学号不能为空")
    private String studentNo;
    @NotNull(message = "答题列表不能为空")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private Long questionId;
        /** 客观题：选项label（单选）或逗号分隔label（多选）；填空：逗号分隔答案；主观：文字内容 */
        private String answerContent;
        /** 主观题贴图作答时的图片路径（由文件上传接口返回） */
        private String answerImagePath;
    }
}
