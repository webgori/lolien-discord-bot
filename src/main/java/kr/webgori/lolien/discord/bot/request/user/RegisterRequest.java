package kr.webgori.lolien.discord.bot.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
  @Schema(description = "이메일")
  @Email
  private String email;

  @Schema(description = "이메일 인증 번호")
  @Size(min = 6, max = 6)
  @NotBlank
  private String emailAuthNumber;

  @Schema(description = "닉네임")
  @Size(min = 2, max = 12)
  @NotBlank
  private String nickname;

  @Schema(description = "비밀번호")
  @Size(min = 8, max = 20)
  @NotBlank
  private String password;

  @Schema(description = "클리앙 아이디")
  @Size(min = 3, max = 12)
  @NotBlank
  private String clienId;

  @Schema(description = "클리앙 아이디 인증 번호")
  @Size(min = 6, max = 6)
  @NotBlank
  private String clienIdAuthNumber;

  @Schema(description = "소환사 이름")
  @Size(min = 2, max = 20)
  @NotBlank
  private String summonerName;
}
