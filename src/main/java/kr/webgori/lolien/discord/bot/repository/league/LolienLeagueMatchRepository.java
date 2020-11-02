package kr.webgori.lolien.discord.bot.repository.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienLeagueMatchRepository extends JpaRepository<LolienLeagueMatch, Integer> {
  boolean existsByGameId(long gameId);

  void deleteByGameId(long gameId);

  List<LolienLeagueMatch> findByGameCreationGreaterThanEqualAndGameCreationLessThanEqual(
      long startTimestamp, long endTimestamp);
}
