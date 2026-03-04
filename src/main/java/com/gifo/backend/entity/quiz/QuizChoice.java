package com.gifo.backend.entity.quiz;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quiz_choice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "choice_id")
    private Long choiceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "choice_text")
    private String choiceText;

    @Column(name = "is_correct")
    private Boolean isCorrect;
}
