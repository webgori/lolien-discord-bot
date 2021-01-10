package kr.webgori.lolien.discord.bot.repository;

import java.util.Optional;
import kr.webgori.lolien.discord.bot.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Integer> {
  Optional<Position> findByPosition(String position);
}
