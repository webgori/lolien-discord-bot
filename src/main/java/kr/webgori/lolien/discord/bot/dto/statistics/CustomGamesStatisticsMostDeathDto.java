package kr.webgori.lolien.discord.bot.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostDeathDto {
  private long gameId;
  private String summonerName;
  private int deaths;
}
