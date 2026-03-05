package com.gifo.backend.entity.quiz;

import com.gifo.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Quiz extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long quizId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_event_id", nullable = false)
    private QuizEvent quizEvent;

    @Column(name = "question")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type")
    private QuizType quizType;

    @Column(name = "hint")
    private String hint;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "play_limit")
    private Integer playLimit;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY)
    private List<QuizChoice> quizChoices = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY)
    private List<QuizAttempt> quizAttempts = new ArrayList<>();
}
