package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MostWinningDto {
  private String championName;
  private String championUrl;
  private float winRate;
  private long totalPlayedCount;
}
