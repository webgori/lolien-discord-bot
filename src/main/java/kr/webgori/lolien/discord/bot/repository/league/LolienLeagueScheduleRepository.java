package kr.webgori.lolien.discord.bot.repository.league;

import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienLeagueScheduleRepository
    extends JpaRepository<LolienLeagueSchedule, Integer> {

}
