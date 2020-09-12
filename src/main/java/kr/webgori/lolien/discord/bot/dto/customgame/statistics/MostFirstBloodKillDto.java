package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MostFirstBloodKillDto {
  private String summonerName;
  private int count;

  public void increaseCount() {
    this.count = this.count + 1;
  }
}
