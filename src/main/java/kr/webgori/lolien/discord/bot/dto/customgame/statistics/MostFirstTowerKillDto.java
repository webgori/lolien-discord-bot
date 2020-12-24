package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MostFirstTowerKillDto {
  private String summonerName;
  private int count;

  public void increaseCount() {
    this.count = this.count + 1;
  }
}
