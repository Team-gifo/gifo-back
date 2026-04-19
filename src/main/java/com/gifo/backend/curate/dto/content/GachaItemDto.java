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
public class GachaItemDto {

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("image_url")
    private String imageUrl;

    private Double percent;

    @JsonProperty("percent_open")
    private Boolean percentOpen;
}
