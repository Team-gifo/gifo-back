package com.gifo.backend.dto.quiz;

/**
 * 퀴즈 도메인 응답 DTO
 */
public class QuizResponse {

    /**
     * POST /events/{eventUrl}/quiz/submit 응답
     * 채점 결과 + 획득 선물 반환
     */
    public record Submit(
            int correctCount,       // 맞춘 문항 수
            int totalCount,         // 전체 문항 수
            String giftName,        // 획득 선물 이름
            String giftImageUrl     // 획득 선물 이미지
    ) {}
}
