package kr.webgori.lolien.discord.bot.repository.user;

import kr.webgori.lolien.discord.bot.entity.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

}
