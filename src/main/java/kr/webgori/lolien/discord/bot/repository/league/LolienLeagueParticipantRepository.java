package kr.webgori.lolien.discord.bot.repository.league;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienLeagueParticipantRepository
    extends JpaRepository<LolienLeagueParticipant, Integer> {

  List<LolienParticipant> findByLolienSummonerSummonerName(String summonerName);

  List<LolienParticipant> findByLolienSummonerSummonerNameAndChampionId(
      String summonerName, int championId);
}
