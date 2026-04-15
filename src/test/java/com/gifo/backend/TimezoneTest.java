package com.gifo.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JVM 기본 타임존이 Asia/Seoul로 설정되었는지 검증
 * BackendApplication static 블록에서 TimeZone.setDefault() 호출
 */
class TimezoneTest {

    @Test
    @DisplayName("JVM 기본 타임존이 Asia/Seoul인지 확인")
    void defaultTimeZoneIsSeoul() {
        // BackendApplication 클래스 로드 → static 블록 실행
        Class<?> clazz = BackendApplication.class;

        TimeZone defaultTz = TimeZone.getDefault();
        assertEquals("Asia/Seoul", defaultTz.getID(),
                "JVM 기본 타임존이 Asia/Seoul이어야 합니다. 현재: " + defaultTz.getID());
    }

    @Test
    @DisplayName("LocalDateTime.now()가 Asia/Seoul 기준 시각과 일치하는지 확인")
    void localDateTimeNowMatchesSeoul() {
        Class<?> clazz = BackendApplication.class;

        LocalDateTime localNow = LocalDateTime.now();
        ZonedDateTime seoulNow = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

        // 초 단위로 비교 (실행 시간 차이 허용)
        assertEquals(seoulNow.getHour(), localNow.getHour(),
                "LocalDateTime.now()의 시(hour)가 Asia/Seoul 기준과 일치해야 합니다.");
        assertEquals(seoulNow.getMinute(), localNow.getMinute(),
                "LocalDateTime.now()의 분(minute)이 Asia/Seoul 기준과 일치해야 합니다.");
    }
}
