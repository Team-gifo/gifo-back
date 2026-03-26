package com.gifo.backend.dto.bgm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BgmPreset {
    EXCITING("exciting", "신나는", "bgm/preset/exciting.mp3"),
    CALM("calm", "잔잔한", "bgm/preset/calm.mp3"),
    NOSTALGIC("nostalgic", "추억", "bgm/preset/nostalgic.mp3");

    private final String id;
    private final String name;
    /** CDN 도메인 없이 경로만 저장 — 서비스에서 cdnDomain + "/" + path 로 조합 */
    private final String path;
}
