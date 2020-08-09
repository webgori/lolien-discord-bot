package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LolienUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienUserRepository extends JpaRepository<LolienUser, Integer> {
  boolean existsByClienId(String clienId);

  LolienUser findByClienId(String clienId);
}
