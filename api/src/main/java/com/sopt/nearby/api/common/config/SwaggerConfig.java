package com.sopt.nearby.api.common.config;

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
        devServer.setUrl("/"); // API 서버 설정

        //ToDO 운영서버 생길 시 아래에 설정
//        Server prodServer = new Server();
//        prodServer.setUrl("운영 URL"); // 운영서버에 따로 띄우기 위한 서버 추가 가능

        Info info = new Info()
                .title("Nearby API")
                .version("1.0.0")
                .description("Nearby 서버 API 명세서 입니다.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }
}