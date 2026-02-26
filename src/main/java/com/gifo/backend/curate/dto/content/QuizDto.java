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
public class QuizDto {

    @JsonProperty("success_reward")
    private QuizRewardDto successReward;

    @JsonProperty("fail_reward")
    private QuizRewardDto failReward;

    private List<QuizItemDto> list;
}
