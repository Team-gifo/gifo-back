package com.gifo.backend.curate;

import com.gifo.backend.curate.dto.CurateResponseDto;
import com.gifo.backend.curate.dto.CurateImageEnrichRequestDto;
import com.gifo.backend.curate.dto.SurveyRequestDto;
import com.gifo.backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/curate")
@Tag(name = "큐레이션 API", description = "설문 기반 AI 개인화 큐레이션")
@RequiredArgsConstructor
public class CurateController {

    private final CurateService curateService;

    @PostMapping("/create")
    @Operation(summary = "큐레이션 생성", description = "설문(관계/상황/톤/연령/이름)을 받아 Azure OpenAI로 프론트엔드용 JSON 템플릿을 생성합니다.")
    public ApiResponse<CurateResponseDto> curate(@Valid @RequestBody SurveyRequestDto request) {
        CurateResponseDto data = curateService.curate(request);
        return ApiResponse.success("큐레이션 생성 완료", data);
    }

    @PostMapping("/create-base")
    @Operation(summary = "기본 큐레이션 생성(이미지 제외)", description = "텍스트/게임/갤러리 구조만 먼저 생성합니다.")
    public ApiResponse<CurateResponseDto> curateBase(@Valid @RequestBody SurveyRequestDto request) {
        CurateResponseDto data = curateService.curateWithoutImages(request);
        return ApiResponse.success("기본 큐레이션 생성 완료", data);
    }

    @PostMapping("/enrich-images")
    @Operation(summary = "큐레이션 이미지 생성", description = "기본 큐레이션의 gallery 항목에 이미지 URL(data URI 포함)을 바인딩합니다.")
    public ApiResponse<CurateResponseDto> enrichImages(@Valid @RequestBody CurateImageEnrichRequestDto request) {
        CurateResponseDto data = curateService.enrichImages(request.getSurvey(), request.getCurate());
        return ApiResponse.success("큐레이션 이미지 생성 완료", data);
    }
}
