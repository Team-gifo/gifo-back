package com.gifo.backend.entity.quiz;

import com.gifo.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 퀴즈 문제별 최종 결과 (정답/오답)
 * 프론트에서 정답을 맞추거나 playLimit을 소진했을 때 저장
 * 재접속 시 answerHistory 복원용
 */
@Entity
@Table(name = "quiz_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class QuizAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_answer_id")
    private Long quizAnswerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_event_id", nullable = false)
    private QuizEvent quizEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "is_correct", nullable = false)
    private Boolean correct;
}
