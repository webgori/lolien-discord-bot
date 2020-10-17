package kr.webgori.lolien.discord.bot.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyClienIdRequest {
  @Schema(description = "클리앙 아이디")
  @Size(max = 12)
  private String clienId;
}
