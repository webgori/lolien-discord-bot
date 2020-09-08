package kr.webgori.lolien.discord.bot.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostPlayedChampionDto {
  private String championName;
  private String championUrl;
  private int count;

  public void increaseCount() {
    this.count = this.count + 1;
  }
}
