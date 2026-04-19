package com.gifo.backend.curate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "갤러리 항목 (추억 갤러리 섹션)")
public class GalleryItemDto {

    @Schema(description = "사진 제목")
    private String title;

    @JsonProperty("image_url")
    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "사진에 대한 설명 문구")
    private String description;
}
