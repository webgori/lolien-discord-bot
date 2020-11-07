package kr.webgori.lolien.discord.bot.response.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.dto.league.team.TeamDto;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TeamResponse {
  private List<TeamDto> teams;
}
