package com.sopt.nearby.shared.adapter.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080"); // 로컬 개발 서버 절대 경로
        devServer.setDescription("Local Development Server");

        Info info = new Info()
                .title("Nearby API")
                .version("1.0.0")
                .description("Nearby 서버 API 명세서 입니다.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
}