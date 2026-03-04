package com.gifo.backend.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * DB 단순 조회 시 발생하는 예외를 일관되게 처리하는 유틸리티.
 * 새 도메인 추가 시 해당 Repository를 주입하고 get{Entity}OrThrow 메서드를 추가합니다.
 *
 * 사용 예시:
 * Member member = entityFinder.getMemberOrThrow(memberId);
 */
@Component
@RequiredArgsConstructor
public class EntityFinder {

}
