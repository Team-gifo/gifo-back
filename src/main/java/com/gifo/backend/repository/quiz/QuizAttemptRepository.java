package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.QuizAttempt;
import com.gifo.backend.entity.quiz.QuizEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    void deleteByQuizEvent(QuizEvent quizEvent);
}
