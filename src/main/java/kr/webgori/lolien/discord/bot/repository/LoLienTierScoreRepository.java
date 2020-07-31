package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LoLienTierScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienTierScoreRepository extends JpaRepository<LoLienTierScore, String> {
  LoLienTierScore findByTier(String tier);
}
