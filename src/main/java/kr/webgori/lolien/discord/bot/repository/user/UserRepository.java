package kr.webgori.lolien.discord.bot.repository.user;

import java.util.List;
import java.util.Optional;
import kr.webgori.lolien.discord.bot.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailAndUserRoleRoleRole(String email, String role);

  List<User> findByEmailIs(String email);
}
