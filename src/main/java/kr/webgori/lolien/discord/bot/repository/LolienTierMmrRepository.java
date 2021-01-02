package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LolienTierMmr;
import kr.webgori.lolien.discord.bot.entity.LolienTierScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienTierMmrRepository extends JpaRepository<LolienTierMmr, String> {
  LolienTierMmr findByTier(String tier);
}
