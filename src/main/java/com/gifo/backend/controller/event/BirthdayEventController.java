package com.gifo.backend.controller.event;

import com.gifo.backend.dto.event.BirthdayEventCreateRequest;
import com.gifo.backend.dto.event.BirthdayEventCreateResponse;
import com.gifo.backend.dto.event.EventResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.event.BirthdayEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@Tag(name = "Birthday Event API", description = "생일 이벤트 관련 API")
@RequiredArgsConstructor
public class BirthdayEventController {

    private final BirthdayEventService birthdayEventService;

    @PostMapping
    @Operation(summary = "생일 이벤트 생성", description = "갤러리, 가차, 퀴즈, 언박싱 콘텐츠를 포함한 생일 이벤트를 생성합니다.")
    public ResponseEntity<ApiResponse<BirthdayEventCreateResponse>> createEvent(
            @RequestBody BirthdayEventCreateRequest request) {

        BirthdayEventCreateResponse response = birthdayEventService.createEvent(request);
        return ResponseEntity.ok(ApiResponse.success("이벤트가 생성되었습니다.", response));
    }

    /**
     * GET /events/{eventUrl} - 이벤트 전체 콘텐츠 조회
     * URL 접속 시 갤러리 + 이벤트 타입(가차/퀴즈/언박싱) 한번에 반환
     */
    @GetMapping("/{eventUrl}")
    @Operation(summary = "이벤트 조회", description = "URL로 이벤트 전체 콘텐츠(갤러리 + 이벤트)를 조회합니다.")
    public ResponseEntity<ApiResponse<EventResponse.Content>> getEvent(
            @PathVariable String eventUrl) {

        EventResponse.Content response = birthdayEventService.getEventContent(eventUrl);
        return ResponseEntity.ok(ApiResponse.success("이벤트 조회 성공", response));
    }

    /**
     * DELETE /events/{eventUrl}/progress - 진행 데이터 초기화
     * 재접속 시 "다시 시작하겠습니까?" → 뽑기/퀴즈 이력 전체 삭제
     */
    @DeleteMapping("/{eventUrl}/progress")
    @Operation(summary = "진행 데이터 초기화", description = "이벤트 진행 이력(뽑기/퀴즈 시도)을 초기화합니다.")
    public ResponseEntity<ApiResponse<Void>> resetProgress(
            @PathVariable String eventUrl) {

        birthdayEventService.resetProgress(eventUrl);
        return ResponseEntity.ok(ApiResponse.success("진행 데이터가 초기화되었습니다."));
    }
}
