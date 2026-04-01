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

    /**
     * POST /events/{eventUrl}/quiz/answer 응답
     * 서버 채점 결과 + 남은 시도 횟수
     * remainingAttempts=0이면 해당 문제 종료 (정답이든 횟수 소진이든)
     */
    public record Answer(
            Long quizId,
            boolean correct,
            int remainingAttempts,
            int currentQuizIndex
    ) {}
}
