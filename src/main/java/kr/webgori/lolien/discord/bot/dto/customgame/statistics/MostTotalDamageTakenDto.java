package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MostTotalDamageTakenDto {
  private long gameId;
  private String summonerName;
  private long totalDamageTaken;
}
