package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MostTotalDamageTakenDto {
  private long gameId;
  private String summonerName;
  private long totalDamageTaken;
}
