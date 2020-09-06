package kr.webgori.lolien.discord.bot.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostWinningDto {
  private String championName;
  private float winRate;
  private long totalPlayedCount;
}
