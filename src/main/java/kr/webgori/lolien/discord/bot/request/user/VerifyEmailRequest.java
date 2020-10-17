package kr.webgori.lolien.discord.bot.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import lombok.Data;

@Data
public class VerifyEmailRequest {
  @Schema(description = "이메일")
  @Email
  private String email;
}
