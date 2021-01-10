package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Integer> {
  League findByLolienSummonerAndSeason(LolienSummoner lolienSummoner, String season);

  List<League> findByLolienSummoner(LolienSummoner lolienSummoner);

  void deleteByLolienSummoner(LolienSummoner lolienSummoner);
}
