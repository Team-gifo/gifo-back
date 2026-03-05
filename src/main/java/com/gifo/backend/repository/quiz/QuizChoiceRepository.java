package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, Long> {
}
