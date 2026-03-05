package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.QuizEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizEventRepository extends JpaRepository<QuizEvent, Long> {
}
