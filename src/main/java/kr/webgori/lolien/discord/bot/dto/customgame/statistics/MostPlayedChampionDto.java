package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MostPlayedChampionDto {
  private String championName;
  private String championUrl;
  private int count;

  public void increaseCount() {
    this.count = this.count + 1;
  }
}
