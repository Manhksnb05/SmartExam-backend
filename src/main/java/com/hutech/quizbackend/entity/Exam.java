package com.hutech.quizbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "exams")
@Data // Nếu Mạnh dùng Lombok, nếu không hãy tự Generate Getter/Setter nhé
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // Tên bộ đề (thường lấy theo tên file Mạnh tải lên)

    @Column(name = "total_questions")
    private Integer totalQuestions; // Tổng số câu hỏi trong bộ đề này

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Thiết lập mối quan hệ 1 bộ đề có nhiều câu hỏi
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // Tránh lỗi vòng lặp vô hạn khi trả về JSON cho React
    private List<Question> questions;

    // Hàm tự động gán ngày tạo khi lưu
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}