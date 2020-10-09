package kr.webgori.lolien.discord.bot.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AccessTokenRequest {
  @Schema(description = "이메일")
  @Email
  @Size(max = 50)
  private String email;
}
