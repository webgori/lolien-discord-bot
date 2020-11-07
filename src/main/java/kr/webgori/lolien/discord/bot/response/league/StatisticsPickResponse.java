package kr.webgori.lolien.discord.bot.response.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.dto.league.statistics.PickTeamDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class StatisticsPickResponse {
  private List<PickTeamDto> teams;
}
