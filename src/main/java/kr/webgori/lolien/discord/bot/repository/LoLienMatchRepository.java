package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LoLienMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienMatchRepository extends JpaRepository<LoLienMatch, Integer> {
  boolean existsByGameId(long matchId);

  List<LoLienMatch> findTop5AllByOrderByGameCreationDesc();

  void deleteByGameId(long gameId);
}