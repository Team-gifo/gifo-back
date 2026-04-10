package com.gifo.backend.dto.capsule;

import java.time.LocalDateTime;

/**
 * 캡슐 도메인 응답 DTO
 */
public class CapsuleResponse {

    /**
     * POST /events/{eventUrl}/capsules/draw 응답
     * 뽑기 결과로 획득한 선물 정보 (단건)
     * drawnAt: 당첨 시각 (기프티콘 프레임 이미지 생성 시 표기용)
     */
    public record Draw(
            Long capsuleId,
            String giftName,
            String giftImageUrl,
            String description,
            LocalDateTime drawnAt
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
