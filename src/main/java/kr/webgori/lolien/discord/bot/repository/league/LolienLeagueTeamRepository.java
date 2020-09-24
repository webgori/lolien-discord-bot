package kr.webgori.lolien.discord.bot.repository.league;

import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeam;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienLeagueTeamRepository extends JpaRepository<LolienLeagueTeam, Integer> {

}
