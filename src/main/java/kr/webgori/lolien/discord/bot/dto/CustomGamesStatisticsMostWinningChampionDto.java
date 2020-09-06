package kr.webgori.lolien.discord.bot.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostWinningChampionDto {
  private String championName;
  private boolean win;
}
