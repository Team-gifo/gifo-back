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

    @Setter
    @Column(name = "last_correct_count")
    private Integer lastCorrectCount;

    @Setter
    @Column(name = "last_success")
    private Boolean lastSuccess;

    /**
     * 현재 풀고 있는 문제의 남은 시도 횟수
     * 문제를 풀다가 중간에 나갔을 때 이어하기용
     * null이면 아직 해당 문제를 시작하지 않은 상태
     */
    @Setter
    @Column(name = "current_quiz_remaining_attempts")
    private Integer currentQuizRemainingAttempts;

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
