package kr.webgori.lolien.discord.bot.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {
  @Schema(description = "accessToken")
  @NotBlank
  private String accessToken;

  @Schema(description = "refreshToken")
  @NotBlank
  private String refreshToken;
}
