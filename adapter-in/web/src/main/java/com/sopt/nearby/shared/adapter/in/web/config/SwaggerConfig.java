package com.sopt.nearby.shared.adapter.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Nearby API")
                .version("1.0.0")
                .description("Nearby 서버 API 명세서 입니다.");

        return new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerAuthScheme()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");
    }

    @Bean
    public OpenApiCustomizer optionalAuthenticationForPublicReads() {
        return api -> List.of(
                "/api/companion-posts",
                "/api/solo-dining/places",
                "/api/solo-dining/places/{placeId}"
        ).forEach(path -> {
            var item = api.getPaths().get(path);
            if (item != null && item.getGet() != null) {
                item.getGet().setSecurity(List.of(
                        new SecurityRequirement(),
                        new SecurityRequirement().addList(BEARER_AUTH)
                ));
            }
        });
    }
}
