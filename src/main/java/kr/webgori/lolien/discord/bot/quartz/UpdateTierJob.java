package kr.webgori.lolien.discord.bot.quartz;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.component.ConfigComponent;
import kr.webgori.lolien.discord.bot.component.SummonerComponent;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeagueEntry;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@Slf4j
@Component
class UpdateTierJob implements Job {
  private static final String REDIS_UPDATE_TIERS_KEY = "lolien-discord-bot:update-tiers";

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private LolienSummonerRepository lolienSummonerRepository;

  @Autowired
  private LeagueRepository leagueRepository;

  @Override
  public void execute(JobExecutionContext context) {
    String currentSeason = SummonerComponent.getCurrentSeason();

    SetOperations<String, Object> setOperations = redisTemplate.opsForSet();
    long summonersCount = lolienSummonerRepository.count();

    Set<Object> members = Optional
        .ofNullable(setOperations.members(REDIS_UPDATE_TIERS_KEY))
        .orElseGet(Sets::newHashSet);

    int size = members.size();
    if (summonersCount <= size) {
      redisTemplate.delete(REDIS_UPDATE_TIERS_KEY);
    }

    List<LolienSummoner> summoners;

    if (members.isEmpty()) {
      summoners = lolienSummonerRepository.findTop5By();
    } else {
      Set<Integer> memberIdxSet = members
          .stream()
          .map(idx -> Integer.parseInt((String) idx))
          .collect(Collectors.toSet());
      summoners = lolienSummonerRepository.findTop5ByIdxNotIn(memberIdxSet);
    }

    String riotApiKey = ConfigComponent.getRiotApiKey();
    ApiConfig config = new ApiConfig().setKey(riotApiKey);
    RiotApi riotApi = new RiotApi(config);

    for (LolienSummoner lolienSummoner : summoners) {
      int summonerIdx = lolienSummoner.getIdx();
      setOperations.add(REDIS_UPDATE_TIERS_KEY, String.valueOf(summonerIdx));

      String accountId = lolienSummoner.getAccountId();

      try {
        Summoner summoner = riotApi.getSummonerByAccount(Platform.KR, accountId);

        int summonerLevel = lolienSummoner.getSummonerLevel();
        int summonerLevelApi = summoner.getSummonerLevel();

        if (summonerLevel != summonerLevelApi) {
          lolienSummoner.setSummonerLevel(summonerLevelApi);
        }

        String summonerName = lolienSummoner.getSummonerName();
        String summonerNameApi = summoner.getName().replaceAll("\\s+", "");

        if (!summonerName.equals(summonerNameApi)) {
          lolienSummoner.setSummonerName(summonerNameApi);
        }

        lolienSummonerRepository.save(lolienSummoner);

        String summonerId = lolienSummoner.getId();

        Set<LeagueEntry> leagueEntrySet = riotApi
            .getLeagueEntriesBySummonerId(Platform.KR, summonerId);

        List<LeagueEntry> leagueEntries = Lists.newArrayList(leagueEntrySet);
        List<League> leagues = lolienSummoner.getLeagues();

        for (League league : leagues) {
          String season = league.getSeason();

          if (season.equals(currentSeason)) {
            String defaultTier = SummonerComponent.getDefaultTier();

            for (LeagueEntry leagueEntry : leagueEntries) {
              if (leagueEntry.getQueueType().equals("RANKED_SOLO_5x5")) {
                defaultTier = leagueEntry.getTier() + "-" + leagueEntry.getRank();
              }
            }

            league.setTier(defaultTier);
            leagueRepository.save(league);
          }
        }
      } catch (RiotApiException e) {
        logger.error("", e);
      }
    }
  }
}