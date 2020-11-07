package kr.webgori.lolien.discord.bot.dto.league.statistics;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PickSummonerDto {
  private String summonerName;
  private List<PickChampDto> champs;
}
