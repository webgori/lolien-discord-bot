package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LoLienSeasonCompensation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoLienSeasonCompensationRepository
    extends JpaRepository<LoLienSeasonCompensation, String> {

  LoLienSeasonCompensation findBySeason(String season);
}
