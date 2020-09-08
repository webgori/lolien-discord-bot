package kr.webgori.lolien.discord.bot.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CustomGamesStatisticsMostKillDeathAssistInfoDto {
  private String summonerName;
  private int kills;
  private int deaths;
  private int assists;

  public void plusKills(int kill) {
    this.kills += kill;
  }

  public void plusDeaths(int death) {
    this.deaths += death;
  }

  public void plusAssists(int assist) {
    this.assists += assist;
  }
}
