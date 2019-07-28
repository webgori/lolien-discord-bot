package kr.webgori.lolien.discord.bot.response;

import kr.webgori.lolien.discord.bot.entity.league.LoLienLeague;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class LeagueGetLeaguesResponse {
    private List<LoLienLeague> leagues;
}