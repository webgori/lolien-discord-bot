package kr.webgori.lolien.discord.bot.repository.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LoLienMatch;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeagueMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienLeagueMatchRepository extends JpaRepository<LoLienLeagueMatch, Integer> {
  boolean existsByGameId(long matchId);

  List<LoLienMatch> findTop5AllByOrderByGameCreationDesc();

  void deleteByGameId(long gameId);
}