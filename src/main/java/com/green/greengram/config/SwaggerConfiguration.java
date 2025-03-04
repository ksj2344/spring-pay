package com.green.greengram.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(
        title = "${constants.swagger.info.title}"
        , description = "${constants.swagger.info.description}"
        , version = "${constants.swagger.info.version}"
    )
    , security = @SecurityRequirement(name = "${constants.swagger.authorization.name}")
)

@SecurityScheme(
    type = SecuritySchemeType.HTTP
    , name = "${constants.swagger.authorization.name}"
    , in = SecuritySchemeIn.HEADER
    , bearerFormat = "${constants.swagger.authorization.bearer-format}"
    , scheme = "${constants.swagger.authorization.scheme}"
)
@Configuration
public class SwaggerConfiguration {
    @Bean
    public GroupedOpenApi groupAllApi() {
        return GroupedOpenApi.builder()
                .group("All")
                .packagesToScan("com.green.greengram")
                .build();
    }

    @Bean
    public GroupedOpenApi groupAuthApi() {
        return GroupedOpenApi.builder()
                .group("Auth") //Select a definition에 나타날 그룹 이름
                .pathsToMatch("/api/user/sign-up", "/api/user/sign-in", "/api/user/access-token") //uri를 통해 설정
                .build(); // /api/어쩌고/**: /api/어쩌고 다음에 뭐가오든 다 가져옴
    }

    @Bean
    public GroupedOpenApi groupGreengramApi() {
        return GroupedOpenApi.builder()
                .group("Greengram")
                .group("Greengram") //Select a definition에 나타날 그룹 이름
                .pathsToMatch("/api/feed/**", "/api/user", "/api/user/pic", "/api/product/**")
                .build();
    }

    @Bean
    public GroupedOpenApi groupPayApi() {
        return GroupedOpenApi.builder()
                .group("Pay")
                .packagesToScan("com.green.greengram.kakaopay")  //pakage로도 설정 가능!
                .build();
    }

    @Bean
    public GroupedOpenApi groupCommonApi() {
        return GroupedOpenApi.builder()
                .group("Common")
                .pathsToMatch("/api/common/**")
                .build();
    }

    //swagger에서 카테고리 별로 그룹화하여 볼 수 있게 설정
}
