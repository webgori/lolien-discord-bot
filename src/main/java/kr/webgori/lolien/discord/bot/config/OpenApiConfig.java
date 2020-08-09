package kr.webgori.lolien.discord.bot.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(title = "lolien-discord-bot API 명세서",
        description = "API 명세서",
        version = "v1",
        contact = @Contact(name = "webgori", email = "webgori@gmail.com"),
        license = @License(name = "Apache 2.0",
            url = "http://www.apache.org/licenses/LICENSE-2.0.html")
    )
)
@SecurityScheme(
    name = "JWT",
    description = "JWT authentication with bearer token",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "Bearer [token]"
)
@Configuration
public class OpenApiConfig {
  /**
   * customGameOpenApi.
   * @return GroupedOpenApi
   */
  @Bean
  public GroupedOpenApi customGameOpenApi() {
    String[] paths = {"/**/custom-game/**"};
    return GroupedOpenApi
        .builder()
        .setGroup("내전 관련 API")
        .pathsToMatch(paths)
        .build();
  }

  /**
   * leagueOpenApi.
   * @return GroupedOpenApi
   */
  @Bean
  public GroupedOpenApi leagueOpenApi() {
    String[] paths = {"/league/**"};
    return GroupedOpenApi
        .builder()
        .setGroup("리그 관련 API")
        .pathsToMatch(paths)
        .build();
  }

  /**
   * leagueOpenApi.
   * @return GroupedOpenApi
   */
  @Bean
  public GroupedOpenApi lolienUserOpenApi() {
    String[] paths = {"/**/users/**"};
    return GroupedOpenApi
        .builder()
        .setGroup("롤리앙 유저 관련 API")
        .pathsToMatch(paths)
        .build();
  }
}
