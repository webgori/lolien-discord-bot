package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MostGoldEarnedDto {
  private long gameId;
  private String summonerName;
  private long goldEarned;
}
