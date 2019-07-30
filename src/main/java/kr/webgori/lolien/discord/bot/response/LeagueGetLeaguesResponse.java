package kr.webgori.lolien.discord.bot.response;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeague;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LeagueGetLeaguesResponse {
  private List<LoLienLeague> leagues;
}