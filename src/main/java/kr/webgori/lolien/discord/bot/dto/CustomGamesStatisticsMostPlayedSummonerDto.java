package kr.webgori.lolien.discord.bot.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostPlayedSummonerDto {
  private String summonerName;
  private int count;

  public void increaseCount() {
    this.count = this.count + 1;
  }
}
