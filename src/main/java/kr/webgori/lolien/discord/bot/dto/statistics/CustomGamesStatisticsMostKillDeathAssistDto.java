package kr.webgori.lolien.discord.bot.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostKillDeathAssistDto {
  private String summonerName;
  private float kda;
}
