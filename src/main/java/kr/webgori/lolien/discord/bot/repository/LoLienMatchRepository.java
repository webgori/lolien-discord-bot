package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienMatchRepository extends JpaRepository<LolienMatch, Integer> {
  boolean existsByGameId(long matchId);

  List<LolienMatch> findTop5AllByOrderByGameCreationDesc();

  void deleteByGameId(long gameId);
}