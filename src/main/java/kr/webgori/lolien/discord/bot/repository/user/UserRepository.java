package kr.webgori.lolien.discord.bot.repository.user;

import kr.webgori.lolien.discord.bot.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
  boolean existsByEmail(String email);

  User findByEmail(String email);
}
