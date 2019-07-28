package kr.webgori.lolien.discord.bot.repository.league;

import kr.webgori.lolien.discord.bot.entity.LoLienParticipant;
import kr.webgori.lolien.discord.bot.entity.league.LoLienLeagueParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoLienLeagueParticipantRepository extends JpaRepository<LoLienLeagueParticipant, Integer> {
    List<LoLienParticipant> findByLoLienSummonerSummonerName(String summonerName);

    List<LoLienParticipant> findByLoLienSummonerSummonerNameAndChampionId(
            String summonerName, int championId);
}