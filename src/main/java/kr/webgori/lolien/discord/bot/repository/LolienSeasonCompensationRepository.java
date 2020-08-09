package kr.webgori.lolien.discord.bot.repository;

import kr.webgori.lolien.discord.bot.entity.LolienSeasonCompensation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolienSeasonCompensationRepository
    extends JpaRepository<LolienSeasonCompensation, String> {

  LolienSeasonCompensation findBySeason(String season);
}
