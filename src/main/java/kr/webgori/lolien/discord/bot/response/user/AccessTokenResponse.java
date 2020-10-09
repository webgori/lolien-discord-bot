package kr.webgori.lolien.discord.bot.response.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AccessTokenResponse {
  @Schema(description = "accessToken")
  private String accessToken;
}
