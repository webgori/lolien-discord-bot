package kr.webgori.lolien.discord.bot.dto.league.statistics;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PickTeamDto {
  private String teamName;
  private List<PickSummonerDto> summoners;
}
