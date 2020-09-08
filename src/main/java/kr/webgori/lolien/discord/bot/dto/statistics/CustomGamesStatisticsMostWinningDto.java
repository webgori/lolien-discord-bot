package kr.webgori.lolien.discord.bot.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostWinningDto {
  private String championName;
  private String championUrl;
  private float winRate;
  private long totalPlayedCount;
}
