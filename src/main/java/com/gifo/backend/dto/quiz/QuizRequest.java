package com.gifo.backend.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 퀴즈 도메인 요청 DTO
 */
public class QuizRequest {

    /**
     * POST /events/{eventUrl}/quiz/answer 요청
     * 매 시도마다 호출 — 서버가 채점 + remainingAttempts 관리
     */
    public record Answer(
            @NotNull(message = "quizId는 필수입니다.")
            Long quizId,
            @NotBlank(message = "selectedAnswer는 필수입니다.")
            String selectedAnswer
    ) {}
}
