package com.hutech.quizbackend.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PracticeRequestDTO {
    private Long userId;
    private Long examId;
    private List<PracticeAnswerDTO> answers; // Tái sử dụng PracticeAnswerDTO cũ
}
