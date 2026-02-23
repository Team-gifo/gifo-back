package com.gifo.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        servers = {
                @Server(url = "${API_SERVER_URL}", description = "gifo https 서버입니다."),
                @Server(url = "http://localhost:8080", description = "gifo local 서버입니다.")
        }
)
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gifo API 문서")
                        .description("Gifo API 명세입니다.")
                        .version("v1.0.0"));
//                .components(new Components()
//                        .addSecuritySchemes("bearer-key", new SecurityScheme()
//                                .type(SecurityScheme.Type.HTTP)
//                                .scheme("bearer")
//                                .bearerFormat("JWT"))
//                        .addSecuritySchemes("refresh-token", new SecurityScheme()
//                                .type(SecurityScheme.Type.APIKEY)
//                                .in(SecurityScheme.In.HEADER)
//                                .name("Authorization"))
//                );
    }
}