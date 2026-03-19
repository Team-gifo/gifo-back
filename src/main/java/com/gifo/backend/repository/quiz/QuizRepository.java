package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.Quiz;
import com.gifo.backend.entity.quiz.QuizEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    @Query("SELECT DISTINCT q FROM Quiz q LEFT JOIN FETCH q.quizChoices WHERE q.quizEvent = :quizEvent ORDER BY q.sortOrder ASC, q.quizId ASC")
    List<Quiz> findByQuizEventOrderBySortOrderAsc(@Param("quizEvent") QuizEvent quizEvent);
}
