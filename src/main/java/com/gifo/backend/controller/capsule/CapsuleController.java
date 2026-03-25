package com.gifo.backend.controller.capsule;

import com.gifo.backend.dto.capsule.CapsuleRequest;
import com.gifo.backend.dto.capsule.CapsuleResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.capsule.CapsuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
     * POST /events/{eventUrl}/capsules/draw - 캡슐 1회 뽑기
     * 가중치 기반 랜덤 추첨 → 뽑기 이력 저장 → 획득 선물 반환
     */
    @PostMapping("/{eventUrl}/capsules/draw")
    @Operation(summary = "캡슐 뽑기", description = "이벤트 URL로 캡슐을 1개 뽑습니다.")
    public ResponseEntity<ApiResponse<CapsuleResponse.Draw>> draw(
            @PathVariable String eventUrl) {

        CapsuleResponse.Draw response = capsuleService.drawCapsule(eventUrl);
        return ResponseEntity.ok(ApiResponse.success("뽑기 성공", response));
    }

    /**
     * POST /events/{eventUrl}/capsules/select - 뽑힌 캡슐 중 최종 선택
     * 뽑기 이력 중 1개를 선택하여 최종 선물 확정
     */
    @PostMapping("/{eventUrl}/capsules/select")
    @Operation(summary = "캡슐 선택", description = "뽑힌 캡슐 중 1개를 최종 선물로 선택합니다.")
    public ResponseEntity<ApiResponse<CapsuleResponse.Select>> select(
            @PathVariable String eventUrl,
            @Valid @RequestBody CapsuleRequest.Select request) {

        CapsuleResponse.Select response = capsuleService.selectCapsule(eventUrl, request.capsuleId());
        return ResponseEntity.ok(ApiResponse.success("선물 선택 성공", response));
    }
}
