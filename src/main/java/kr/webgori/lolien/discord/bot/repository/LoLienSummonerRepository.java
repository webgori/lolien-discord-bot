package kr.webgori.lolien.discord.bot.repository;

import java.util.List;
import java.util.Set;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienSummonerRepository extends JpaRepository<LoLienSummoner, Integer> {
  boolean existsBySummonerName(String summonerName);

  boolean existsByAccountId(String accountId);

  LoLienSummoner findBySummonerName(String summonerName);

  LoLienSummoner findByAccountId(String accountId);

  List<LoLienSummoner> findTop5ByIdxNotIn(Set<Integer> summonersIdx);

  List<LoLienSummoner> findTop5By();

  List<LoLienSummoner> findTop5ByOrderByMmrDesc();

  long countByIdIn(List<String> ids);
}