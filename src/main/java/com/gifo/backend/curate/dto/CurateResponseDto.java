package com.gifo.backend.curate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gifo.backend.curate.dto.content.ContentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "프론트엔드 바인딩용 큐레이션 결과 (request_body 항목 1개에 해당)")
public class CurateResponseDto {

    @Schema(description = "사용자의 고유 성함 또는 닉네임")
    private String user;

    @JsonProperty("sub_title")
    @Schema(description = "페이지 상단 메인 테마 문구")
    private String subTitle;

    @Schema(description = "BGM 트랙 ID (예: track_sweet_01)")
    private String bgm;

    @Schema(description = "추억 갤러리 목록 (2~3개 권장)")
    private List<GalleryItemDto> gallery;

    @Schema(description = "인터랙티브 콘텐츠 (가챌, 퀴즈, 선물 개봉)")
    private ContentDto content;
}
