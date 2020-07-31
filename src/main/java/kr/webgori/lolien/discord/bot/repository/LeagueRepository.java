package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Integer> {
  League findByLoLienSummonerAndSeason(LoLienSummoner loLienSummoner, String season);

  List<League> findByLoLienSummoner(LoLienSummoner loLienSummoner);
}
