package com.gifo.backend.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "azure.openai")
public class AiProperties {

    private String endpoint;

    private String apiKey;

    private String deploymentName;
}

