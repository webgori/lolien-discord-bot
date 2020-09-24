package kr.webgori.lolien.discord.bot.response.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeam;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TeamResponse {
  private List<LolienLeagueTeam> teams;
}
