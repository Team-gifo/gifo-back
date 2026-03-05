package com.gifo.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${API_SERVER_URL:}")
    private String apiServerUrl;

    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Gifo API 문서")
                        .description("Gifo API 명세입니다.")
                        .version("v1.0.0"));

        if (apiServerUrl != null && !apiServerUrl.isBlank()) {
            openAPI.servers(List.of(
                    new Server().url(apiServerUrl).description("gifo https 서버입니다."),
                    new Server().url("http://localhost:8080").description("gifo local 서버입니다.")
            ));
        }

        return openAPI;
    }
}