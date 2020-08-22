package kr.webgori.lolien.discord.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGameTeamBanDto {
  @Schema(description = "픽 순서")
  private int pickTurn;

  @Schema(description = "챔피언 아이디")
  private int championId;
}