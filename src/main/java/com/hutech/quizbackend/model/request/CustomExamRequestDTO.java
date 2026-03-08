package com.hutech.quizbackend.model.request;

import lombok.Data;

@Data
public class CustomExamRequestDTO {
    private Long originExamId;
    private Integer timeLimit;
    private Integer questionCount;
    private String userEmail;
    private Long userId; // THÊM DÒNG NÀY ĐỂ ĐỊNH DANH NGƯỜI DÙNG
    private String title;
}