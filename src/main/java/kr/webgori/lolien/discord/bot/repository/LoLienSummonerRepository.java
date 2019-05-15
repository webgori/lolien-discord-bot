package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienSummonerRepository extends JpaRepository<LoLienSummoner, Integer> {
  boolean existsBySummonerName(String summonerName);

  boolean existsByAccountId(String accountId);

  LoLienSummoner findBySummonerName(String summonerName);

  LoLienSummoner findByAccountId(String accountId);
}