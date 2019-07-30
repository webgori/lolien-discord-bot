package kr.webgori.lolien.discord.bot.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LeagueGetLeaguesResponse {
  private List<LeagueGetLeagueResponse> leagues;
}