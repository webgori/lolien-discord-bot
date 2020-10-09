package kr.webgori.lolien.discord.bot.repository.user;

import kr.webgori.lolien.discord.bot.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
  Role findByRole(String role);
}
