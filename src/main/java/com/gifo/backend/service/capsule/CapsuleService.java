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
import java.time.LocalDateTime;
import java.util.List;

/**
 * 캡슐 뽑기 비즈니스 로직
 *
 * POST /events/{eventUrl}/capsule/draw 처리:
 * 1. 이벤트 유효성 검증 (ACTIVE 상태인지)
 * 2. 남은 뽑기 횟수 확인 (maxDrawCount 초과 시 예외)
 * 3. 가중치(weight) 기반 랜덤 추첨
 * 4. CapsuleDraw 이력 저장
 * 5. 획득 선물 반환
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CapsuleService {

    private final EntityFinder entityFinder;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleDrawRepository capsuleDrawRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    public CapsuleResponse.Draw drawCapsule(String eventUrl) {
        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        CapsuleEvent capsuleEvent = event.getCapsuleEvent();
        if (capsuleEvent == null) {
            throw new CapsuleException(ErrorCode.CAPSULE_NOT_FOUND);
        }

        // 뽑기 횟수 초과 검증
        long drawCount = capsuleDrawRepository.countByCapsuleEvent(capsuleEvent);
        if (drawCount >= capsuleEvent.getMaxDrawCount()) {
            throw new CapsuleException(ErrorCode.CAPSULE_DRAW_LIMIT_EXCEEDED);
        }

        // 가중치 기반 랜덤 추첨
        List<Capsule> capsules = capsuleRepository.findByCapsuleEvent(capsuleEvent);
        Capsule drawn = weightedRandom(capsules);

        // 뽑기 이력 저장
        capsuleDrawRepository.save(CapsuleDraw.builder()
                .capsuleEvent(capsuleEvent)
                .capsule(drawn)
                .createdAt(LocalDateTime.now())
                .build());

        Gift gift = drawn.getGift();
        return new CapsuleResponse.Draw(
                gift.getGiftName(),
                gift.getGiftImageUrl(),
                gift.getDescription());
    }

    /**
     * 가중치 기반 랜덤 추첨 알고리즘
     * totalWeight 범위 안에서 난수 생성 → 누적합으로 당첨 캡슐 결정
     * 예: [weight=7000, weight=2000, weight=1000] → 70%, 20%, 10%
     */
    private Capsule weightedRandom(List<Capsule> capsules) {
        int totalWeight = capsules.stream().mapToInt(Capsule::getWeight).sum();
        int rand = RANDOM.nextInt(totalWeight);
        int cumulative = 0;
        for (Capsule capsule : capsules) {
            cumulative += capsule.getWeight();
            if (rand < cumulative) return capsule;
        }
        return capsules.get(capsules.size() - 1); // fallback
    }
}
