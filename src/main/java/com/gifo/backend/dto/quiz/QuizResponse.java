package com.gifo.backend.dto.quiz;

/**
 * 퀴즈 도메인 응답 DTO
 */
public class QuizResponse {

    /**
     * POST /events/{eventUrl}/quiz/result 응답
     * 저장된 정답 수 + 성공 여부 반환
     */
    public record Result(
            int correctCount,
            boolean success
    ) {}
}
