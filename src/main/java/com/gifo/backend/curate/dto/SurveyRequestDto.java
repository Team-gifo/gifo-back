package com.gifo.backend.curate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "개인화 큐레이션 설문 요청")
public class SurveyRequestDto {

    @NotBlank(message = "대상과의 관계를 선택해주세요.")
    @Schema(description = "대상과의 관계", example = "연인", allowableValues = {"연인", "친구", "가족", "동료"})
    private String relationship;

    @NotBlank(message = "상황을 선택해주세요.")
    @Schema(description = "상황", example = "생일", allowableValues = {"생일", "축하", "위로", "응원", "사과"})
    private String situation;

    @NotBlank(message = "톤을 선택해주세요.")
    @Schema(description = "톤", example = "감동", allowableValues = {"장난", "감동", "담백"})
    private String tone;

    @NotBlank(message = "대상의 연령을 입력해주세요.")
    @Schema(description = "대상 연령", example = "20대")
    private String targetAge;

    @Schema(description = "대상을 부를 이름 (선택, 개인화 문구에 반영)")
    private String targetName;
}
