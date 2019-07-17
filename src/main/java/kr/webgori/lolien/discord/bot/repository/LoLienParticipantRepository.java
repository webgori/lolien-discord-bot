package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import kr.webgori.lolien.discord.bot.entity.LoLienParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienParticipantRepository extends JpaRepository<LoLienParticipant, Integer> {
  List<LoLienParticipant> findByLoLienSummonerSummonerName(String summonerName);

  List<LoLienParticipant> findByLoLienSummonerSummonerNameAndChampionId(
      String summonerName, int championId);
}