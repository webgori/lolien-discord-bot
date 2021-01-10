package kr.webgori.lolien.discord.bot.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class AlterUserRequest {
  @Schema(description = "닉네임")
  @Size(min = 2, max = 12)
  private String nickname;

  @Schema(description = "비밀번호")
  @Size(max = 20)
  private String currentPassword;

  @Schema(description = "비밀번호")
  @Size(max = 20)
  private String alterPassword;

  @Schema(description = "소환사 이름")
  @Size(min = 2, max = 20)
  @NotBlank
  private String summonerName;

  @Schema(description = "포지션")
  @Size(min = 1, max = 5)
  private List<String> positions;
}
