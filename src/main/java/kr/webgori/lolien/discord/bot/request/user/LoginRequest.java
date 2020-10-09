package kr.webgori.lolien.discord.bot.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
  @Schema(description = "이메일")
  @Email
  @Size(max = 50)
  private String email;

  @Schema(description = "비밀번호")
  @Size(max = 20)
  @NotBlank
  private String password;
}
