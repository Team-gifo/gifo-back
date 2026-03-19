package com.gifo.backend.controller.capsule;

import com.gifo.backend.dto.capsule.CapsuleResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.capsule.CapsuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@Tag(name = "Capsule API", description = "캡슐 뽑기 관련 API")
@RequiredArgsConstructor
public class CapsuleController {

    private final CapsuleService capsuleService;

    /**
     * POST /events/{eventUrl}/capsule/draw - 캡슐 1회 뽑기
     * 가중치 기반 랜덤 추첨 → 획득 선물 반환
     * 뽑기 횟수 초과 시 CAPSULE_DRAW_LIMIT_EXCEEDED 예외
     */
    @PostMapping("/{eventUrl}/capsule/draw")
    @Operation(summary = "캡슐 뽑기", description = "이벤트 URL로 캡슐을 한 번 뽑습니다.")
    public ResponseEntity<ApiResponse<CapsuleResponse.Draw>> draw(
            @PathVariable String eventUrl) {

        CapsuleResponse.Draw response = capsuleService.drawCapsule(eventUrl);
        return ResponseEntity.ok(ApiResponse.success("뽑기 성공", response));
    }
}
