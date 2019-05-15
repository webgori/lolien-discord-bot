package kr.webgori.lolien.discord.bot.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kr.webgori.lolien.discord.bot.entity.Champ;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.repository.ChampRepository;
import kr.webgori.lolien.discord.bot.repository.LeagueRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.league.dto.LeaguePosition;
import net.rithms.riot.api.endpoints.summoner.dto.Summoner;
import net.rithms.riot.constant.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendErrorMessage;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Component
public class TeamGenerateComponent {
  private static final String[] TIER_LIST = {"IRON","BRONZE", "SILVER", "GOLD", "PLATINUM",
          "DIAMOND", "MASTER", "GRANDMASTER", "CHALLENGE"};
  private static final String[] RANK_LIST = {"IV", "III", "II", "I"};
  private static final String CURRENT_SEASON = "S9";
  private static final String DEFAULT_TIER = "UNRANKED";

  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LeagueRepository leagueRepository;
  private final CustomGameComponent customGameComponent;
  private final ChampRepository champRepository;

  @Value("${riot.api.key}")
  private String riotApiKey;

  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();
    String receivedMessage = event.getMessage().getContentDisplay();
    List<String> commands = Lists.newArrayList(receivedMessage.split(" "));

    if (commands.size() < 3) {
      sendSyntax(textChannel);
      return;
    }

    String subCommand = commands.get(1);

    if (!subCommand.equals("밸런스") && !subCommand.equals("랜덤")) {
      sendSyntax(textChannel);
      return;
    }

    StringBuilder entriesBuilder = new StringBuilder();

    for (int i = 2; i < commands.size(); i++) {
      entriesBuilder.append(commands.get(i));
    }

    String[] entries = entriesBuilder.toString().split(",");

    if (entries.length != 10) {
      sendErrorMessage(textChannel, "참가자가 인원이 잘못되었습니다.", Color.RED);
      return;
    }

    List<Object> teamList = generateTeam(textChannel, entries, loLienSummonerRepository);
    String teamA = (String) teamList.get(0);

    for (int i = 0; i < 20; i++) {
      int countOfEntry = StringUtils.countOccurrencesOf(teamA, ",") + 1;
      int pointOfTeamA = (int) teamList.get(1);
      int pointOfTeamB = (int) teamList.get(4);
      int periodPoint = pointOfTeamB - pointOfTeamA;

      if (countOfEntry == 5 && periodPoint < 5) {
        break;
      }

      teamList = generateTeam(textChannel, entries, loLienSummonerRepository);
      teamA = (String) teamList.get(0);
    }

    int countOfEntry = StringUtils.countOccurrencesOf(teamA, ",") + 1;
    int pointOfTeamA = (int) teamList.get(1);
    int pointOfTeamB = (int) teamList.get(4);
    int periodPoint = pointOfTeamB - pointOfTeamA;

    if (countOfEntry == 5 && periodPoint < 5) {
      String teamMessageBuilder = String.valueOf(teamList.get(0)) +
              "\n" +
              teamList.get(3) +
              "\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------" +
              "\n1팀" +
              teamList.get(2) +
              "\n----------------------------------------------------------------------------------------------------------------------------------------------------------------------------" +
              "\n2팀" +
              teamList.get(5);
      sendMessage(textChannel, teamMessageBuilder);
    } else {
      sendErrorMessage(textChannel, "팀 구성이 실패하였습니다.", Color.RED);
    }
  }

  private int getPointByLevel(int level) {
    if (level > 150) {
      return 24;
    } else if (level > 100) {
      return 20;
    } else if (level > 50) {
      return 16;
    } else {
      return 8;
    }
  }

  private List<Object> generateTeam(TextChannel textChannel, String[] entries,
                                    LoLienSummonerRepository loLienSummonerRepository) {
    int totalPoint = 0;
    Map<String, Integer> entriesPointMap = new LinkedHashMap<>();

    for (String summonerName : entries) {
      boolean hasSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format("\"!소환사 등록 %s\" 명령어로 소환사 등록을 먼저 해주시기 바랍니다.", summonerName);
        sendErrorMessage(textChannel, errorMessage, Color.BLUE);
        throw new IllegalArgumentException("register summoner first");
      }

      LoLienSummoner loLienSummoner = loLienSummonerRepository.findBySummonerName(summonerName);

      boolean presentCurrentSeason = loLienSummoner
              .getLeagues()
              .stream()
              .anyMatch(l -> l.getSeason().equals(CURRENT_SEASON));

      if (!presentCurrentSeason) {
        String tier = getCurrentSeasonTier(summonerName);

        League league = League
                .builder()
                .loLienSummoner(loLienSummoner)
                .season(CURRENT_SEASON)
                .tier(tier)
                .build();

        leagueRepository.save(league);
      }

      Map<String, Integer> tiers = getTiers();

      List<League> leagues = loLienSummoner.getLeagues();
      leagues.sort(Comparator.comparing(League::getSeason, Comparator.reverseOrder()));
      String tier;

      if (leagues.size() == 1) {
        League league = leagues.get(0);
        tier = league.getTier();
      } else {
        League currentSeasonLeague = leagues.get(0);
        String currentTier = currentSeasonLeague.getTier();

        League prevSeasonLeague = leagues.get(1);
        String prevTier = prevSeasonLeague.getTier();

        if (currentTier.equals(DEFAULT_TIER)) {
          tier = prevTier;
        } else {
          int currentTierPoint = tiers.get(currentTier);
          int prevTierPoint = tiers.get(prevTier);

          tier = currentTier;

          if (prevTierPoint > currentTierPoint) {
            tier = prevTier;
          }
        }
      }

      int point;

      if (tier.equals("UNRANKED")) {
        int summonerLevel = loLienSummoner.getSummonerLevel();
        point = getPointByLevel(summonerLevel);
      } else {
        point = tiers.get(tier);
      }

      totalPoint += point;

      entriesPointMap.put(summonerName, point);
    }

    int halfPoint = totalPoint / 2;
    List<String> entryList = Arrays
            .stream(entries)
            .collect(Collectors.toList());

    Collections.shuffle(entryList);

    List<Integer> pointListOfTeamA = Lists.newArrayList();
    List<String> summonerListOfTeamA = Lists.newArrayList();
    int pointOfTeamA = 0;

    for (String entrySummoner : entryList) {
      if (pointListOfTeamA.size() < 5) {
        Integer summonerPoint = entriesPointMap.get(entrySummoner);

        if (pointOfTeamA + summonerPoint >= halfPoint) {
          continue;
        }

        pointListOfTeamA.add(summonerPoint);
        summonerListOfTeamA.add(entrySummoner);

        pointOfTeamA = pointListOfTeamA
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
      }
    }

    entryList.removeAll(summonerListOfTeamA);

    StringBuilder teamABuilder = new StringBuilder();
    teamABuilder.append("1팀: ");
    teamABuilder = setTeamSummoner(summonerListOfTeamA, teamABuilder);

    List<Object> team = Lists.newArrayList();
    String teamA = teamABuilder.substring(0, teamABuilder.length() - 2);
    team.add(teamA);
    team.add(pointOfTeamA);

    String teamASummonerMostBuilder = setTeamSummonerMostTop3(summonerListOfTeamA);
    team.add(teamASummonerMostBuilder);

    StringBuilder teamBBuilder = new StringBuilder();
    teamBBuilder.append("2팀: ");
    teamBBuilder = setTeamSummoner(entryList, teamBBuilder);

    String teamB = teamBBuilder.substring(0, teamBBuilder.length() - 2);
    team.add(teamB);
    team.add(totalPoint - pointOfTeamA);

    String teamBSummonerMostBuilder = setTeamSummonerMostTop3(entryList);
    team.add(teamBSummonerMostBuilder);

    return team;
  }

  private StringBuilder setTeamSummoner(List<String> entryList, StringBuilder teamBuilder) {
    Map<String, Integer> tiers = getTiers();

    for (String summoner : entryList) {
      LoLienSummoner loLienSummoner = loLienSummonerRepository.findBySummonerName(summoner);
      List<League> leagues = loLienSummoner.getLeagues();

      leagues.sort(Comparator.comparing(League::getSeason, Comparator.reverseOrder()));
      String tier;

      if (leagues.size() == 1) {
        League league = leagues.get(0);
        tier = league.getTier();
      } else {
        League currentSeasonLeague = leagues.get(0);
        String currentTier = currentSeasonLeague.getTier();

        League prevSeasonLeague = leagues.get(1);
        String prevTier = prevSeasonLeague.getTier();

        if (currentTier.equals(DEFAULT_TIER)) {
          tier = prevTier;
        } else {
          int currentTierPoint = tiers.get(currentTier);
          int prevTierPoint = tiers.get(prevTier);

          tier = currentTier;

          if (prevTierPoint > currentTierPoint) {
            tier = prevTier;
          }
        }
      }

      int tierPoint;

      if (tier.equals(DEFAULT_TIER)) {
        int summonerLevel = loLienSummoner.getSummonerLevel();
        tierPoint = getPointByLevel(summonerLevel);
      } else {
        tierPoint = tiers.get(tier);
      }

      teamBuilder
              .append(summoner)
              .append("(")
              .append(tier)
              .append(" / ")
              .append(tierPoint)
              .append(")")
              .append(", ");
    }

    return teamBuilder;
  }

  private String setTeamSummonerMostTop3(List<String> entryList) {
    StringBuilder stringBuilder = new StringBuilder();

    for (String summoner : entryList) {
      LinkedHashMap<Integer, Long> mostChampions = customGameComponent.getMostChamp(summoner, 3);
      List<String> mostChampionsList = Lists.newArrayList();

      for (Map.Entry<Integer, Long> mostChampion : mostChampions.entrySet()) {
        int champId = mostChampion.getKey();
        Champ champ = champRepository.findByKey(champId);
        String championName = champ.getName();
        mostChampionsList.add(championName);
      }

      String mostChamps = String.join(", ", mostChampionsList);
      String mostSummonerChamps = String.format("%s (%s)", summoner, mostChamps);
      stringBuilder
              .append("\n")
              .append(mostSummonerChamps);
    }

    return stringBuilder.toString();
  }

  private Map<String, Integer> getTiers() {
    Map<String, Integer> tiersMap = new HashMap<>();

    int point = 0;
    for (String tier : TIER_LIST) {
      for (String rank : RANK_LIST) {
        String key = tier + "-" + rank;
        tiersMap.put(key, point);
        point++;
      }
    }

    return tiersMap;
  }

  private String getCurrentSeasonTier(String summonerName) {
    ApiConfig config = new ApiConfig().setKey(riotApiKey);
    RiotApi riotApi = new RiotApi(config);

    try {
      Summoner summoner = riotApi.getSummonerByName(Platform.KR, summonerName);
      String summonerId = summoner.getId();
      Set<LeaguePosition> leaguePositions = riotApi
              .getLeaguePositionsBySummonerId(Platform.KR, summonerId);
      List<LeaguePosition> leaguePositionList = new ArrayList<>(leaguePositions);

      String tier = DEFAULT_TIER;

      for (LeaguePosition leaguePosition : leaguePositionList) {
        if (leaguePosition.getQueueType().equals("RANKED_SOLO_5x5")) {
          tier = leaguePosition.getTier() + "-" + leaguePosition.getRank();
        }
      }

      return tier;
    } catch (RiotApiException e) {
      logger.error("{}", e);
      throw new IllegalArgumentException("RiotApiException");
    }
  }

  private void sendSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !팀구성 밸런스 소환사명1, 소환사명2, 소환사명3 ...", Color.RED);
  }
}