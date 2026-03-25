package com.gifo.backend.service.capsule;

import com.gifo.backend.dto.capsule.CapsuleResponse;
import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.capsule.CapsuleDraw;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.gift.Gift;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.capsule.CapsuleException;
import com.gifo.backend.global.util.EntityFinder;
import com.gifo.backend.repository.capsule.CapsuleDrawRepository;
import com.gifo.backend.repository.capsule.CapsuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

/**
 * 캡슐 뽑기 비즈니스 로직
 *
 * 1. POST /events/{eventUrl}/capsules/draw — 단건 뽑기
 *    비관적 락 → 상태 검증 → 횟수 검증 → 가중치 랜덤 추첨 → CapsuleDraw 저장 → 결과 반환
 *
 * 2. POST /events/{eventUrl}/capsules/select — 최종 선택
 *    뽑힌 캡슐 중 1개를 최종 선물로 확정 (selected=true)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleService {

    private final EntityFinder entityFinder;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleDrawRepository capsuleDrawRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 캡슐 1개 뽑기
     * 가중치 기반 랜덤 추첨 → DB에 CapsuleDraw(selected=false) 저장 → 결과 반환
     */
    public CapsuleResponse.Draw drawCapsule(String eventUrl) {
        // 비관적 락으로 동시 뽑기 요청 방지
        BirthdayEvent event = entityFinder.getEventByUrlForUpdateOrThrow(eventUrl);

        CapsuleEvent capsuleEvent = event.getCapsuleEvent();
        if (capsuleEvent == null) {
            throw new CapsuleException(ErrorCode.CAPSULE_NOT_FOUND);
        }

        // 이미 선택 완료된 경우 뽑기 불가
        if (capsuleDrawRepository.existsByCapsuleEventAndSelectedTrue(capsuleEvent)) {
            throw new CapsuleException(ErrorCode.CAPSULE_ALREADY_SELECTED);
        }

        // 뽑기 횟수 초과 검증
        long drawCount = capsuleDrawRepository.countByCapsuleEvent(capsuleEvent);
        if (drawCount >= capsuleEvent.getMaxDrawCount()) {
            throw new CapsuleException(ErrorCode.CAPSULE_DRAW_LIMIT_EXCEEDED);
        }

        // 이미 뽑힌 캡슐 ID Set으로 필터링 (O(1) lookup)
        List<Capsule> allCapsules = capsuleRepository.findByCapsuleEvent(capsuleEvent);
        Set<Long> drawnIds = capsuleDrawRepository.findDrawnCapsuleIdsByCapsuleEvent(capsuleEvent);
        List<Capsule> remaining = allCapsules.stream()
                .filter(c -> !drawnIds.contains(c.getCapsuleId()))
                .toList();

        if (remaining.isEmpty()) {
            throw new CapsuleException(ErrorCode.CAPSULE_ALL_DRAWN);
        }

        // 가중치 기반 랜덤 추첨
        Capsule drawn = weightedRandom(remaining);

        // 뽑기 이력 저장 (selected=false)
        capsuleDrawRepository.save(CapsuleDraw.builder()
                .capsuleEvent(capsuleEvent)
                .capsule(drawn)
                .build());

        Gift gift = drawn.getGift();
        return new CapsuleResponse.Draw(
                drawn.getCapsuleId(),
                gift.getGiftName(),
                gift.getGiftImageUrl(),
                gift.getDescription());
    }

    /**
     * 뽑힌 캡슐 중 1개 선택 (변경 가능)
     * 기존 선택이 있으면 해제 후 새로 선택
     */
    public CapsuleResponse.Select selectCapsule(String eventUrl, Long capsuleId) {
        BirthdayEvent event = entityFinder.getEventByUrlForUpdateOrThrow(eventUrl);

        CapsuleEvent capsuleEvent = event.getCapsuleEvent();
        if (capsuleEvent == null) {
            throw new CapsuleException(ErrorCode.CAPSULE_NOT_FOUND);
        }

        // 기존 선택이 있으면 모두 해제 (재선택 허용, 다중 결과 안전 처리)
        capsuleDrawRepository.findByCapsuleEventAndSelectedTrue(capsuleEvent)
                .forEach(CapsuleDraw::unselect);

        // 뽑기 이력에서 해당 캡슐 찾기 (fetch join으로 N+1 방지)
        CapsuleDraw draw = entityFinder.getCapsuleDrawWithGiftOrThrow(capsuleEvent, capsuleId);

        draw.select();

        Gift gift = draw.getCapsule().getGift();
        return new CapsuleResponse.Select(
                gift.getGiftName(),
                gift.getGiftImageUrl(),
                gift.getDescription());
    }

    /**
     * 가중치 기반 랜덤 추첨 알고리즘
     * totalWeight 범위 안에서 난수 생성 → 누적합으로 당첨 캡슐 결정
     */
    private Capsule weightedRandom(List<Capsule> capsules) {
        int totalWeight = capsules.stream().mapToInt(Capsule::getWeight).sum();
        if (totalWeight <= 0) {
            return capsules.get(RANDOM.nextInt(capsules.size()));
        }
        int rand = RANDOM.nextInt(totalWeight);
        int cumulative = 0;
        for (Capsule capsule : capsules) {
            cumulative += capsule.getWeight();
            if (rand < cumulative) return capsule;
        }
        return capsules.getLast();
    }
}
