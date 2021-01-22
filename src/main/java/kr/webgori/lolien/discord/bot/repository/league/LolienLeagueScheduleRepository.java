package kr.webgori.lolien.discord.bot.repository.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeague;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienLeagueScheduleRepository
    extends JpaRepository<LolienLeagueSchedule, Integer> {

  List<LolienLeagueSchedule> findByLolienLeagueIdx(int leagueIndex);
}
