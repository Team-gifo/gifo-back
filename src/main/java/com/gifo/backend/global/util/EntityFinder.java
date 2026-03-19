package com.gifo.backend.global.util;

import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.event.EventStatus;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.capsule.CapsuleException;
import com.gifo.backend.global.exception.event.EventException;
import com.gifo.backend.repository.event.BirthdayEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DB 단순 조회 시 발생하는 예외를 일관되게 처리하는 유틸리티.
 * 새 도메인 추가 시 해당 Repository를 주입하고 get{Entity}OrThrow 메서드를 추가합니다.
 *
 * 사용 예시:
 * BirthdayEvent event = entityFinder.getEventOrThrow(eventId);
 * BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);
 * BirthdayEvent event = entityFinder.getEventByUrlForUpdateOrThrow(eventUrl);
 */
@Component
@RequiredArgsConstructor
public class EntityFinder {

    private final BirthdayEventRepository birthdayEventRepository;

    /**
     * eventId(PK)로 이벤트를 단건 조회합니다.
     * 이벤트가 존재하지 않을 경우 전역 EVENT_NOT_FOUND 예외를 발생시킵니다.
     */
    public BirthdayEvent getEventOrThrow(Long eventId) {
        return birthdayEventRepository.findById(eventId)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));
    }

    /**
     * eventUrl로 이벤트를 조회하고, 상태(EXPIRED/DELETED)를 검증합니다.
     * ACTIVE 상태의 이벤트만 정상 반환됩니다.
     */
    public BirthdayEvent getEventByUrlOrThrow(String eventUrl) {
        BirthdayEvent event = birthdayEventRepository.findByEventUrl(eventUrl)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));
        validateEventStatus(event);
        return event;
    }

    /**
     * 비관적 락(Pessimistic Lock, FOR UPDATE)을 적용하여 eventUrl로 이벤트를 조회합니다.
     * 동시성 제어가 필요한 로직(예: 캡슐 뽑기 등 동시에 여러 요청이 들어올 수 있는 상황)에서
     * 안전하게 데이터를 획득하고 변경할 때 사용합니다.
     */
    public BirthdayEvent getEventByUrlForUpdateOrThrow(String eventUrl) {
        return birthdayEventRepository.findByEventUrlForUpdate(eventUrl)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));
    }

    /**
     * 이벤트 상태를 검증합니다. EXPIRED/DELETED 상태면 예외를 던집니다.
     * 비관적 락으로 조회한 이벤트에도 재사용 가능합니다.
     */
    public void validateEventStatus(BirthdayEvent event) {
        if (event.getStatus() == EventStatus.EXPIRED) {
            throw new EventException(ErrorCode.EVENT_EXPIRED);
        }
        if (event.getStatus() == EventStatus.DELETED) {
            throw new EventException(ErrorCode.EVENT_DELETED);
        }
    }
}
