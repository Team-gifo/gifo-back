package com.gifo.backend.dto.capsule;

/**
 * 캡슐 도메인 응답 DTO
 */
public class CapsuleResponse {

    /**
     * POST /events/{eventUrl}/capsule/draw 응답
     * 뽑기 결과로 획득한 선물 정보
     */
    public record Draw(
            String giftName,
            String giftImageUrl,
            String description
    ) {}
}
