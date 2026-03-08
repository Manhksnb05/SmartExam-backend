package com.hutech.quizbackend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_question_stats")
@Data
public class UserQuestionStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam; // Lưu thêm Exam để truy vấn cho nhanh khi tạo CustomExam

    @Column(name = "wrong_count")
    private int wrongCount = 0; // Đếm số lần làm sai

    @Column(name = "correct_count")
    private int correctCount = 0; // Đếm số lần làm đúng
}