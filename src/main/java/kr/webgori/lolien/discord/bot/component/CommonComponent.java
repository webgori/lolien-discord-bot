package kr.webgori.lolien.discord.bot.component;

import java.util.Objects;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSeasonCompensation;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienTierScore;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSeasonCompensationRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.LolienTierScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommonComponent {
  private static final String DEFAULT_TIER = "UNRANKED";

  private final LolienSummonerRepository lolienSummonerRepository;
  private final LeagueRepository leagueRepository;
  private final LolienSeasonCompensationRepository lolienSeasonCompensationRepository;
  private final LolienTierScoreRepository lolienTierScoreRepository;

  void checkExistsMmr(LolienSummoner lolienSummoner) {
    Integer mmr = lolienSummoner.getMmr();

    if (Objects.isNull(mmr)) {
      initSummerMmr(lolienSummoner);
    }
  }

  private void initSummerMmr(LolienSummoner lolienSummoner) {
    float season08Mmr = getSummonerMmrBySeason(lolienSummoner, "S08");
    float season09Mmr = getSummonerMmrBySeason(lolienSummoner, "S09");
    float season10Mmr = getSummonerMmrBySeason(lolienSummoner, "S10");

    int mmr = (int) (season08Mmr + season09Mmr + season10Mmr);

    lolienSummoner.setMmr(mmr);
    lolienSummonerRepository.save(lolienSummoner);
  }

  private float getSummonerMmrBySeason(LolienSummoner lolienSummoner, String season) {
    League league = leagueRepository.findByLolienSummonerAndSeason(lolienSummoner, season);

    if (Objects.isNull(league)) {
      return 0;
    }

    String tier = league.getTier();

    if (tier.equals(DEFAULT_TIER)) {
      return 0;
    }

    LolienTierScore tierScore = lolienTierScoreRepository.findByTier(tier);
    Integer score = tierScore.getScore();

    LolienSeasonCompensation season08Compensation = lolienSeasonCompensationRepository
        .findBySeason(season);

    Float compensationValue = season08Compensation.getCompensationValue();

    return score * compensationValue;
  }
}
