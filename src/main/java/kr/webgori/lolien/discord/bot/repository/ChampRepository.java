package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.Champ;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChampRepository extends JpaRepository<Champ, Integer> {
  Champ findByKey(int key);
}