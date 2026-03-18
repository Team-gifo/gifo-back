package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.Quiz;
import com.gifo.backend.entity.quiz.QuizEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByQuizEventOrderBySortOrderAsc(QuizEvent quizEvent);
}
