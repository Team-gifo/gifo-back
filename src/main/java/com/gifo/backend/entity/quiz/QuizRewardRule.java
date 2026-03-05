package com.gifo.backend.entity.quiz;

import com.gifo.backend.entity.BaseEntity;
import com.gifo.backend.entity.gift.Gift;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "quiz_reward_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class QuizRewardRule extends BaseEntity {

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
}
