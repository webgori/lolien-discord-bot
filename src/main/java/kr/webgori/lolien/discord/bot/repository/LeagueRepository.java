package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Integer> {
}
