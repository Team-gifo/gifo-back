package com.gifo.backend.entity.quiz;

import com.gifo.backend.entity.gift.Gift;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quiz_reward_rule")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizRewardRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_event_id", nullable = false)
    private QuizEvent quizEvent;

    @Column(name = "min_correct")
    private Integer minCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private Gift gift;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
