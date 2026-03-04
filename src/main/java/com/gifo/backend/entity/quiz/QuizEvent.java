package com.gifo.backend.entity.quiz;

import com.gifo.backend.entity.event.BirthdayEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_event_id")
    private Long quizEventId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private BirthdayEvent birthdayEvent;

    @Column(name = "total_attempt")
    private Integer totalAttempt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "quizEvent", fetch = FetchType.LAZY)
    private List<Quiz> quizzes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "quizEvent", fetch = FetchType.LAZY)
    private List<QuizRewardRule> quizRewardRules = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "quizEvent", fetch = FetchType.LAZY)
    private List<QuizAttempt> quizAttempts = new ArrayList<>();
}
