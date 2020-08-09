package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LolienTierScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienTierScoreRepository extends JpaRepository<LolienTierScore, String> {
  LolienTierScore findByTier(String tier);
}
