package com.gifo.backend.curate.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class UnboxingDto {

    @JsonProperty("before_open")
    private UnboxingPhaseDto beforeOpen;

    @JsonProperty("after_open")
    private UnboxingAfterDto afterOpen;
}
