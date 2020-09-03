package kr.webgori.lolien.discord.bot.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostBannedDto {
  private String championName;
  private int count;

  public void increaseCount() {
    this.count = this.count + 1;
  }
}
