package com.hutech.quizbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.List;

@Entity
@Table(name = "questions")
@Data
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question; // Nội dung câu hỏi trích xuất từ AI

    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options; // Danh sách 4 đáp án A, B, C, D

    @Column(columnDefinition = "TEXT")
    private String answer; // Đáp án đúng (được AI xác định hoặc Mạnh sửa)

    // Thiết lập mối quan hệ N-1 với bảng Exams (Yêu cầu 2)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    @JsonBackReference // Ngăn chặn vòng lặp vô hạn khi trả về JSON
    private Exam exam;

    // Getter và Setter (Nếu Mạnh không dùng Lombok thì tự tạo nhé)
}