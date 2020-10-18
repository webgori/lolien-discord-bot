package kr.webgori.lolien.discord.bot.repository.user;

import kr.webgori.lolien.discord.bot.entity.user.ClienUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienUserRepository extends JpaRepository<ClienUser, Integer> {
  boolean existsByClienId(String clienId);
}
