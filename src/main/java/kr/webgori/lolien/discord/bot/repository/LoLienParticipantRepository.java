package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienParticipantRepository extends JpaRepository<LolienParticipant, Integer> {
  List<LolienParticipant> findByLolienSummonerSummonerName(String summonerName);

  List<LolienParticipant> findByLolienSummonerSummonerNameAndChampionId(
      String summonerName, int championId);
}