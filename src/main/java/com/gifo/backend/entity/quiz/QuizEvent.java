package com.gifo.backend.entity.quiz;

import com.gifo.backend.entity.BaseEntity;
import com.gifo.backend.entity.event.BirthdayEvent;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class QuizEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_event_id")
    private Long quizEventId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private BirthdayEvent birthdayEvent;

    @Setter
    @Column(name = "total_attempt")
    private Integer totalAttempt;

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
