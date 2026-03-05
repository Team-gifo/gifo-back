package com.gifo.backend.controller.event;

import com.gifo.backend.dto.event.BirthdayEventCreateRequest;
import com.gifo.backend.dto.event.BirthdayEventCreateResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.event.BirthdayEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
