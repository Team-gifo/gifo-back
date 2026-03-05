package com.gifo.backend.dto.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BirthdayEventCreateResponse {

    /** 생성된 이벤트 ID */
    private Long eventId;

    /** 생일 주인공이 접속할 고유 URL 코드 */
    private String eventUrl;
}
