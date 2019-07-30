package kr.webgori.lolien.discord.bot.repository.league;

import kr.webgori.lolien.discord.bot.entity.league.LoLienLeague;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienLeagueRepository extends JpaRepository<LoLienLeague, Integer> {
  boolean existsByIdx(long leagueIdx);
}