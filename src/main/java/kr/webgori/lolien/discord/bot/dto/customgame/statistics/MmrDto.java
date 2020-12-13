package kr.webgori.lolien.discord.bot.dto.customgame.statistics;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MmrDto {
  private String summonerName;
  private int mmr;
}
