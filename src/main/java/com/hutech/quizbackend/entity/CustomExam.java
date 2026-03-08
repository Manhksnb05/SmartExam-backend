package com.hutech.quizbackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "custom_exams")
@Data
public class CustomExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private Integer timeLimit;
    private Integer questionCount;

    @Column(columnDefinition = "TEXT")
    private String selectedQuestionIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_exam_id")
    @JsonIgnore
    private Exam originExam;

    // Quan hệ 1-N: 1 Custom_Exam có thể có nhiều người giải (Results)
    @OneToMany(mappedBy = "customExam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @ToString.Exclude
    private List<Result> results;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "active")
    private Boolean active;
}