package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.QuizEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizEventRepository extends JpaRepository<QuizEvent, Long> {

    @Modifying(clearAutomatically = true)
    @Query("UPDATE QuizEvent q SET q.totalAttempt = q.totalAttempt + 1 WHERE q.quizEventId = :id")
    void incrementTotalAttempt(@Param("id") Long quizEventId);
}
