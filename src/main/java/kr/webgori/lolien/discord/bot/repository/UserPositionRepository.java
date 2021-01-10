package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.UserPosition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPositionRepository extends JpaRepository<UserPosition, Integer> {
  void deleteByLolienSummoner(LolienSummoner lolienSummoner);
}
