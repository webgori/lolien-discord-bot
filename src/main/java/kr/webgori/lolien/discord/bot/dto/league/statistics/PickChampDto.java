package kr.webgori.lolien.discord.bot.dto.league.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PickChampDto {
  private int champId;
  private String champName;
  private int pickCount;

  public void increasePickCount() {
    this.pickCount += 1;
  }
}
