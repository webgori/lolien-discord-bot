package kr.webgori.lolien.discord.bot.component;

import java.util.Objects;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LoLienSeasonCompensation;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.entity.LoLienTierScore;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSeasonCompensationRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienTierScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommonComponent {
  private static final String DEFAULT_TIER = "UNRANKED";

  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LeagueRepository leagueRepository;
  private final LoLienSeasonCompensationRepository loLienSeasonCompensationRepository;
  private final LoLienTierScoreRepository loLienTierScoreRepository;

  void checkExistsMmr(LoLienSummoner loLienSummoner) {
    Integer mmr = loLienSummoner.getMmr();

    if (Objects.isNull(mmr)) {
      initSummerMmr(loLienSummoner);
    }
  }

  private void initSummerMmr(LoLienSummoner loLienSummoner) {
    float season08Mmr = getSummonerMmrBySeason(loLienSummoner, "S08");
    float season09Mmr = getSummonerMmrBySeason(loLienSummoner, "S09");
    float season10Mmr = getSummonerMmrBySeason(loLienSummoner, "S10");

    int mmr = (int) (season08Mmr + season09Mmr + season10Mmr);

    loLienSummoner.setMmr(mmr);
    loLienSummonerRepository.save(loLienSummoner);
  }

  private float getSummonerMmrBySeason(LoLienSummoner loLienSummoner, String season) {
    League league = leagueRepository.findByLoLienSummonerAndSeason(loLienSummoner, season);

    if (Objects.isNull(league)) {
      return 0;
    }

    String tier = league.getTier();

    if (tier.equals(DEFAULT_TIER)) {
      return 0;
    }

    LoLienTierScore tierScore = loLienTierScoreRepository.findByTier(tier);
    Integer score = tierScore.getScore();

    LoLienSeasonCompensation season08Compensation = loLienSeasonCompensationRepository
        .findBySeason(season);

    Float compensationValue = season08Compensation.getCompensationValue();

    return score * compensationValue;
  }
}
