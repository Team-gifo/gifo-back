package com.gifo.backend.repository.quiz;

import com.gifo.backend.entity.quiz.Quiz;
import com.gifo.backend.entity.quiz.QuizChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizChoiceRepository extends JpaRepository<QuizChoice, Long> {

    List<QuizChoice> findByQuiz(Quiz quiz);
}
