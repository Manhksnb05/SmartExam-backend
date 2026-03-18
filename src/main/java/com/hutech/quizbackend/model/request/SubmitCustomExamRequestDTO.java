package com.hutech.quizbackend.model.request;

import com.hutech.quizbackend.model.dto.AnswerSubmitDTO;
import lombok.Data;
import java.util.List;

@Data
public class SubmitCustomExamRequestDTO {
    private Long userId;
    private Long customExamId;
    private List<AnswerSubmitDTO> answers; // Tái sử dụng lại DTO từng câu hỏi đã tạo trước đó
    // HỨNG THỜI GIAN LÀM BÀI (TÍNH BẰNG GIÂY)
    private Integer timeTakenSeconds;
}