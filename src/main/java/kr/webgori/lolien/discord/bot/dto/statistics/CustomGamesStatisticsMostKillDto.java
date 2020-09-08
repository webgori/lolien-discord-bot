package kr.webgori.lolien.discord.bot.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostKillDto {
  private String summonerName;
  private int kills;

  public void plusKills(int kill) {
    this.kills += kill;
  }
}
