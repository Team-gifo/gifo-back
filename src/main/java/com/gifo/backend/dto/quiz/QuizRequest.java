package com.gifo.backend.dto.quiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 퀴즈 도메인 요청 DTO
 */
public class QuizRequest {

    /**
     * POST /events/{eventUrl}/quiz/result 요청
     * 프론트에서 채점 완료 후 호출 (서버가 DB에서 정답 수 계산)
     */
    public record Result(int correctCount) {}

    /**
     * POST /events/{eventUrl}/quiz/answer 요청
     * 문제 1개 풀이 결과 저장
     */
    public record Answer(
            @NotNull(message = "quizId는 필수입니다.")
            Long quizId,
            boolean correct,
            @Min(value = 0, message = "시도 횟수는 0 이상이어야 합니다.")
            int remainingAttempts
    ) {}
}
