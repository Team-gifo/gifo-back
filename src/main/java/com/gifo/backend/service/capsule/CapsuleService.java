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
import com.gifo.backend.repository.event.BirthdayEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 캡슐 뽑기 비즈니스 로직
 *
 * POST /events/{eventUrl}/capsule/draw 처리:
 * 1. 비관적 락으로 동시 요청 방지
 * 2. 이벤트 유효성 검증 (ACTIVE 상태인지)
 * 3. 남은 뽑기 횟수 확인 (maxDrawCount 초과 시 예외)
 * 4. 가중치(weight) 기반 랜덤 추첨 (비복원 추출)
 * 5. CapsuleDraw 이력 저장
 * 6. 획득 선물 반환
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
     * @param requestCount 한 번에 뽑을 횟수
     * @return 뽑힌 선물 리스트
     */
    public List<CapsuleResponse.Draw> drawCapsules(String eventUrl, int requestCount) {
        // 비관적 락으로 동시 뽑기 요청 방지
        BirthdayEvent event = entityFinder.getEventByUrlForUpdateOrThrow(eventUrl);

        CapsuleEvent capsuleEvent = event.getCapsuleEvent();
        if (capsuleEvent == null) {
            throw new CapsuleException(ErrorCode.CAPSULE_NOT_FOUND);
        }

        // 뽑기 횟수 초과 검증
        long currentDrawCount = capsuleDrawRepository.countByCapsuleEvent(capsuleEvent);
        if (currentDrawCount + requestCount > capsuleEvent.getMaxDrawCount()) {
            throw new CapsuleException(ErrorCode.CAPSULE_DRAW_LIMIT_EXCEEDED);
        }

        // 이미 뽑힌 캡슐 ID Set으로 필터링 (O(1) lookup)
        List<Capsule> allCapsules = capsuleRepository.findByCapsuleEvent(capsuleEvent);
        Set<Long> drawnIds = capsuleDrawRepository.findDrawnCapsuleIdsByCapsuleEvent(capsuleEvent);

        List<Capsule> remaining = allCapsules.stream()
                .filter(c -> !drawnIds.contains(c.getCapsuleId()))
                .collect(Collectors.toCollection(ArrayList::new));

        // 남은 캡슐 수보다 뽑으려는 개수가 많으면 예외 처리
        if (remaining.size() < requestCount) {
            throw new CapsuleException(ErrorCode.CAPSULE_ALL_DRAWN);
        }

        List<CapsuleDraw> newDraws = new ArrayList<>();
        List<CapsuleResponse.Draw> results = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            Capsule drawn = weightedRandom(remaining);

            // 캡슐은 남은 목록에서 즉시 제거하여 중복 당첨 방지
            remaining.remove(drawn);

            newDraws.add(CapsuleDraw.builder()
                    .capsuleEvent(capsuleEvent)
                    .capsule(drawn)
                    .createdAt(LocalDateTime.now())
                    .build());

            // 응답 DTO 목록에 추가
            Gift gift = drawn.getGift();
            results.add(new CapsuleResponse.Draw(
                    gift.getGiftName(),
                    gift.getGiftImageUrl(),
                    gift.getDescription()));
        }

        capsuleDrawRepository.saveAll(newDraws);

        return results;
    }

    /**
     * 가중치 기반 랜덤 추첨 알고리즘
     * totalWeight 범위 안에서 난수 생성 → 누적합으로 당첨 캡슐 결정
     */
    private Capsule weightedRandom(List<Capsule> capsules) {
        int totalWeight = capsules.stream().mapToInt(Capsule::getWeight).sum();
        if (totalWeight <= 0) {
            // 가중치가 모두 0인 경우 균등 확률로 임의 캡슐 반환 (의도된 fallback)
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
