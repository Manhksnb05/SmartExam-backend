package com.hutech.quizbackend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "questions")
@Data
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @ElementCollection
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text")
    private List<String> options;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "difficulty", columnDefinition = "integer default 0")
    private Integer difficulty = 0; // 0: Dễ, 1: Trung bình, 2: Khó (Mặc định là 0)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    @JsonBackReference // Phía con (Question) sẽ không gọi ngược lại cha (Exam)
    private Exam exam;
}