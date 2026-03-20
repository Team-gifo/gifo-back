package com.gifo.backend.dto.capsule;

/**
 * 캡슐 도메인 요청 DTO
 */
public class CapsuleRequest {

    /**
     * POST /events/{eventUrl}/capsules/select 요청
     * 뽑힌 캡슐 중 최종 선택
     */
    public record Select(
            Long capsuleId
    ) {}
}
