package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.Quiz;
import com.gifo.backend.entity.quiz.QuizAnswer;
import com.gifo.backend.entity.quiz.QuizEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    @EntityGraph(attributePaths = {"quiz"})
    List<QuizAnswer> findByQuizEventOrderByQuizAnswerIdAsc(QuizEvent quizEvent);

    long countByQuizEventAndCorrectTrue(QuizEvent quizEvent);

    long countByQuizEvent(QuizEvent quizEvent);

    boolean existsByQuizEventAndQuiz(QuizEvent quizEvent, Quiz quiz);

    void deleteByQuizEvent(QuizEvent quizEvent);
}
