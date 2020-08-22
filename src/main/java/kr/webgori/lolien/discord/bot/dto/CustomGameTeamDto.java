package kr.webgori.lolien.discord.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGameTeamDto {
  private boolean win;

  @Schema(description = "100: blue, 200: red")
  private int teamId;

  private List<CustomGameTeamBanDto> bans;
}
