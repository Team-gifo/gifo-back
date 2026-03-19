package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.QuizEvent;
import com.gifo.backend.entity.quiz.QuizRewardRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRewardRuleRepository extends JpaRepository<QuizRewardRule, Long> {

    List<QuizRewardRule> findByQuizEventOrderByMinCorrectDesc(QuizEvent quizEvent);
}
