package com.gifo.backend.dto.capsule;

/**
 * 캡슐 도메인 응답 DTO
 */
public class CapsuleResponse {

    /**
     * POST /events/{eventUrl}/capsules/draw 응답
     * 뽑기 결과로 획득한 선물 정보 (단건)
     */
    public record Draw(
            Long capsuleId,
            String giftName,
            String giftImageUrl,
            String description
    ) {}

    /**
     * POST /events/{eventUrl}/capsules/select 응답
     * 최종 선택한 선물 정보
     */
    public record Select(
            String giftName,
            String giftImageUrl,
            String description
    ) {}
}
