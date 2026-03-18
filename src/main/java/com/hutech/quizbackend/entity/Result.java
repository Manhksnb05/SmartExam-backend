package com.hutech.quizbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "results")
@Data
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custom_exam_id")
    @JsonIgnore
    private CustomExam customExam;

    @Column(nullable = false)
    private Double score;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "time_taken")
    private Integer timeTaken; // Lưu thời gian làm bài thực tế (tính bằng giây)

    @Column(name = "completed_at")
    private LocalDateTime completedAt = LocalDateTime.now();

    @Column(name = "active")
    private Boolean active;
}