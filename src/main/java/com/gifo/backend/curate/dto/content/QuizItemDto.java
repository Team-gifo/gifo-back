package com.gifo.backend.curate.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class QuizItemDto {

    @JsonProperty("quiz_id")
    private Integer quizId;

    private String type;

    private String title;

    @JsonProperty("image_url")
    private String imageUrl;

    private String description;

    private String hint;

    private List<String> options;

    private List<String> answer;

    @JsonProperty("play_limit")
    private Integer playLimit;
}
