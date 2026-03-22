package com.gifo.backend.dto.quiz;

/**
 * 퀴즈 도메인 요청 DTO
 */
public class QuizRequest {

    /**
     * POST /events/{eventUrl}/quiz/result 요청
     * 프론트에서 채점 완료 후 최종 정답 수만 전송
     */
    public record Result(int correctCount) {}

    /**
     * POST /events/{eventUrl}/quiz/answer 요청
     * 문제 1개 풀이 결과 저장
     */
    public record Answer(
            Long quizId,
            boolean correct,
            int remainingAttempts
    ) {}
}
