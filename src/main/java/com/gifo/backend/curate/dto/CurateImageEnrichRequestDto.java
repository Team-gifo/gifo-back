package com.gifo.backend.curate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "큐레이션 결과에 이미지 생성만 추가하기 위한 요청")
public class CurateImageEnrichRequestDto {

    @Valid
    @NotNull(message = "설문 정보(survey)는 필수입니다.")
    private SurveyRequestDto survey;

    @Valid
    @NotNull(message = "큐레이션 결과(curate)는 필수입니다.")
    private CurateResponseDto curate;
}

