package kr.webgori.lolien.discord.bot.repository.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeague;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienLeagueRepository extends JpaRepository<LolienLeague, Integer> {
  List<LolienLeague> findAllByOrderByIdxDesc();

  boolean existsByIdx(long leagueIdx);
}
