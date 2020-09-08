package kr.webgori.lolien.discord.bot.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostAssistDto {
  private String summonerName;
  private int assists;

  public void plusAssists(int assists) {
    this.assists += assists;
  }
}
