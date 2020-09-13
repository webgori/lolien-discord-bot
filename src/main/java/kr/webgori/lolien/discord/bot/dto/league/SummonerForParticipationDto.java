package kr.webgori.lolien.discord.bot.dto.league;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SummonerForParticipationDto {
  private String summonerName;
  private int numberOfParticipation;

  public void increaseNumberOfParticipation() {
    this.numberOfParticipation += 1;
  }
}
