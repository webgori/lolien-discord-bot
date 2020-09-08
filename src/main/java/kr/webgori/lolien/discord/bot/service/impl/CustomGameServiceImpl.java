package kr.webgori.lolien.discord.bot.service.impl;

import static java.util.Collections.reverseOrder;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getEndDateOfMonth;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getStartDateOfMonth;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.localDateToTimestamp;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.timestampToLocalDateTime;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.component.RiotComponent;
import kr.webgori.lolien.discord.bot.dto.ChampDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamBanDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamDto;
import kr.webgori.lolien.discord.bot.dto.DataDragonVersionDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMatchDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostAssistDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostBannedDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostDeathDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostKillDeathAssistDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostKillDeathAssistInfoDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostKillDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostPlayedChampionDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostPlayedSummonerDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostVisionScoreDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostWinningChampionDto;
import kr.webgori.lolien.discord.bot.dto.statistics.CustomGamesStatisticsMostWinningDto;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienParticipantStats;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienTeamBans;
import kr.webgori.lolien.discord.bot.entity.LolienTeamStats;
import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LolienMatchRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.response.CustomGameResponse;
import kr.webgori.lolien.discord.bot.response.CustomGamesResponse;
import kr.webgori.lolien.discord.bot.response.CustomGamesStatisticsResponse;
import kr.webgori.lolien.discord.bot.service.CustomGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomGameServiceImpl implements CustomGameService {
  private static final int BLUE_TEAM = 100;
  private static final int RED_TEAM = 200;

  private final LolienMatchRepository lolienMatchRepository;
  private final CustomGameComponent customGameComponent;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final RiotComponent riotComponent;

  @Transactional
  @Override
  public void addResult(CustomGameAddResultRequest customGameAddResultRequest) {
    long matchId = customGameAddResultRequest.getMatchId();
    String entriesString = customGameAddResultRequest.getEntries();

    boolean existsByGameId = lolienMatchRepository.existsByGameId(matchId);

    if (existsByGameId) {
      throw new IllegalArgumentException("이미 등록된 리그 결과 입니다.");
    }

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      throw new IllegalArgumentException("게임 참여 인원이 잘못 되었습니다.");
    }

    customGameComponent.addResult(matchId, entries);
  }

  @Transactional(readOnly = true)
  @Override
  public CustomGamesResponse getCustomGames(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);

    Page<LolienMatch> lolienMatchePages = lolienMatchRepository
        .findByOrderByGameCreationDesc(pageRequest);
    List<LolienMatch> lolienMatches = lolienMatchePages.getContent();

    int totalPages = lolienMatchePages.getTotalPages();

    return getCustomGamesResponse(lolienMatches, totalPages);
  }

  @Transactional(readOnly = true)
  @Override
  public CustomGamesResponse getCustomGamesBySummoner(String targetSummonerName,
                                                      int page, int size) {

    LolienSummoner lolienSummoner = lolienSummonerRepository.findBySummonerName(targetSummonerName);

    if (Objects.isNull(lolienSummoner)) {
      throw new SummonerNotFoundException("");
    }

    List<LolienMatch> lolienMatches = lolienSummoner.getParticipants()
        .stream()
        .map(LolienParticipant::getMatch)
        .sorted(Comparator.comparing(LolienMatch::getGameCreation).reversed())
        .collect(Collectors.toList());

    int skip = page * size;

    List<LolienMatch> lolienMatchPages = lolienMatches
        .stream()
        .skip(skip)
        .limit(size)
        .collect(Collectors.toList());

    int totalPages = lolienMatches.size() / size;

    return getCustomGamesResponse(lolienMatchPages, totalPages);
  }

  private CustomGamesResponse getCustomGamesResponse(List<LolienMatch> lolienMatches,
                                                     int totalPages) {

    List<CustomGameResponse> customGames = Lists.newArrayList();
    List<CustomGameTeamDto> teamDtoList = Lists.newArrayList();
    List<CustomGameTeamBanDto> teamBanDtoList = Lists.newArrayList();

    List<DataDragonVersionDto> dataDragonVersions = riotComponent.getDataDragonVersions();
    Map<String, JsonObject> summonerJsonObjectMap = Maps.newHashMap();
    Map<String, JsonObject> championsJsonObjectMap = Maps.newHashMap();
    Map<String, JsonObject> itemsJsonObjectMap = Maps.newHashMap();
    Map<String, JsonArray> runesJsonArrayMap = Maps.newHashMap();

    for (LolienMatch lolienMatch : lolienMatches) {
      int idx = lolienMatch.getIdx();
      long gameCreation = lolienMatch.getGameCreation();
      long gameDuration = lolienMatch.getGameDuration();
      long gameId = lolienMatch.getGameId();
      String gameMode = lolienMatch.getGameMode();
      String gameType = lolienMatch.getGameType();
      String gameVersion = lolienMatch.getGameVersion();
      int mapId = lolienMatch.getMapId();
      String platformId = lolienMatch.getPlatformId();
      int queueId = lolienMatch.getQueueId();
      int seasonId = lolienMatch.getSeasonId();

      List<LolienParticipant> participants = lolienMatch
          .getParticipants()
          .stream()
          .sorted(Comparator.comparing(LolienParticipant::getIdx))
          .collect(Collectors.toList());

      List<CustomGameSummonerDto> blueTeamSummoners = Lists.newArrayList();
      List<CustomGameSummonerDto> redTeamSummoners = Lists.newArrayList();

      for (LolienParticipant lolienParticipant : participants) {
        String closeDataDragonVersion = riotComponent.getCloseDataDragonVersion(gameVersion,
            dataDragonVersions);

        JsonObject summonerJsonObject;

        if (summonerJsonObjectMap.containsKey(closeDataDragonVersion)) {
          summonerJsonObject = summonerJsonObjectMap.get(closeDataDragonVersion);
        } else {
          summonerJsonObject = riotComponent.getSummonerJsonObject(closeDataDragonVersion);
          summonerJsonObjectMap.put(closeDataDragonVersion, summonerJsonObject);
        }

        JsonObject championsJsonObject;

        if (championsJsonObjectMap.containsKey(closeDataDragonVersion)) {
          championsJsonObject = championsJsonObjectMap.get(closeDataDragonVersion);
        } else {
          championsJsonObject = riotComponent.getChampionJsonObject(closeDataDragonVersion);
          championsJsonObjectMap.put(closeDataDragonVersion, championsJsonObject);
        }

        int championId = lolienParticipant.getChampionId();
        String championUrl = riotComponent.getChampionUrl(championsJsonObject,
            closeDataDragonVersion, championId);
        String championName = riotComponent.getChampionName(championsJsonObject, championId);

        LolienParticipantStats lolienParticipantStats = lolienParticipant.getStats();

        long totalDamageDealtToChampions = lolienParticipantStats.getTotalDamageDealtToChampions();
        int wardsPlaced = lolienParticipantStats.getWardsPlaced();

        int kills = lolienParticipantStats.getKills();
        int deaths = lolienParticipantStats.getDeaths();
        int assists = lolienParticipantStats.getAssists();

        int champLevel = lolienParticipantStats.getChampLevel();
        int totalMinionsKilled = lolienParticipantStats.getTotalMinionsKilled();

        JsonObject itemsJsonObject;

        if (itemsJsonObjectMap.containsKey(closeDataDragonVersion)) {
          itemsJsonObject = itemsJsonObjectMap.get(closeDataDragonVersion);
        } else {
          itemsJsonObject = riotComponent.getItemJsonObject(closeDataDragonVersion);
          itemsJsonObjectMap.put(closeDataDragonVersion, itemsJsonObject);
        }

        int item0 = lolienParticipantStats.getItem0();
        String item0Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item0);
        String item0Name = riotComponent.getItemName(itemsJsonObject, item0);
        String item0Description = riotComponent
            .getItemDescription(itemsJsonObject, item0);

        int item1 = lolienParticipantStats.getItem1();
        String item1Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item1);
        String item1Name = riotComponent.getItemName(itemsJsonObject, item1);
        String item1Description = riotComponent
            .getItemDescription(itemsJsonObject, item1);

        int item2 = lolienParticipantStats.getItem2();
        String item2Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item2);
        String item2Name = riotComponent.getItemName(itemsJsonObject, item2);
        String item2Description = riotComponent
            .getItemDescription(itemsJsonObject, item2);

        int item3 = lolienParticipantStats.getItem3();
        String item3Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item3);
        String item3Name = riotComponent.getItemName(itemsJsonObject, item3);
        String item3Description = riotComponent
            .getItemDescription(itemsJsonObject, item3);

        int item4 = lolienParticipantStats.getItem4();
        String item4Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item4);
        String item4Name = riotComponent.getItemName(itemsJsonObject, item4);
        String item4Description = riotComponent
            .getItemDescription(itemsJsonObject, item4);

        int item5 = lolienParticipantStats.getItem5();
        String item5Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item5);
        String item5Name = riotComponent.getItemName(itemsJsonObject, item5);
        String item5Description = riotComponent
            .getItemDescription(itemsJsonObject, item5);

        int item6 = lolienParticipantStats.getItem6();
        String item6Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item6);
        String item6Name = riotComponent.getItemName(itemsJsonObject, item6);
        String item6Description = riotComponent
            .getItemDescription(itemsJsonObject, item6);

        JsonArray runesJsonArray;

        if (runesJsonArrayMap.containsKey(closeDataDragonVersion)) {
          runesJsonArray = runesJsonArrayMap.get(closeDataDragonVersion);
        } else {
          runesJsonArray = riotComponent.getRuneJsonArray(closeDataDragonVersion);
          runesJsonArrayMap.put(closeDataDragonVersion, runesJsonArray);
        }

        int primaryRunId = lolienParticipantStats.getPerk0();
        String primaryRuneUrl = riotComponent.getRuneUrl(runesJsonArray, primaryRunId);
        String primaryRuneName = riotComponent.getRuneName(runesJsonArray, primaryRunId);
        String primaryRuneDescription = riotComponent
            .getRuneDescription(runesJsonArray, primaryRunId);

        int subRunId = lolienParticipantStats.getPerkSubStyle();
        String subRuneUrl = riotComponent.getRuneUrl(runesJsonArray, subRunId);
        String subRuneName = riotComponent.getRuneName(runesJsonArray, subRunId);
        String subRuneDescription = riotComponent
            .getRuneDescription(runesJsonArray, subRunId);

        int teamId = lolienParticipant.getTeamId();
        boolean win = lolienParticipantStats.getWin();

        LolienSummoner lolienSummoner = lolienParticipant.getLolienSummoner();
        int lolienSummonerIdx = lolienSummoner.getIdx();
        String summonerName = lolienSummoner.getSummonerName();

        int spell1Id = lolienParticipant.getSpell1Id();
        String spell1Url = riotComponent.getSpellUrl(summonerJsonObject, closeDataDragonVersion,
            spell1Id);
        String spell1Name = riotComponent.getSpellName(summonerJsonObject, spell1Id);
        String spell1Description = riotComponent.getSpellDescription(summonerJsonObject, spell1Id);

        int spell2Id = lolienParticipant.getSpell2Id();
        String spell2Url = riotComponent.getSpellUrl(summonerJsonObject, closeDataDragonVersion,
            spell2Id);
        String spell2Name = riotComponent.getSpellName(summonerJsonObject, spell2Id);
        String spell2Description = riotComponent.getSpellDescription(summonerJsonObject, spell2Id);

        CustomGameSummonerDto customGameSummonerDto = CustomGameSummonerDto
            .builder()
            .idx(lolienSummonerIdx)
            .summonerName(summonerName)
            .championId(championId)
            .championUrl(championUrl)
            .championName(championName)
            .totalDamageDealtToChampions(totalDamageDealtToChampions)
            .spell1Url(spell1Url)
            .spell2Url(spell2Url)
            .spell1Name(spell1Name)
            .spell2Name(spell2Name)
            .spell1Description(spell1Description)
            .spell2Description(spell2Description)
            .kills(kills)
            .deaths(deaths)
            .assists(assists)
            .champLevel(champLevel)
            .totalMinionsKilled(totalMinionsKilled)
            .item0Url(item0Url)
            .item1Url(item1Url)
            .item2Url(item2Url)
            .item3Url(item3Url)
            .item4Url(item4Url)
            .item5Url(item5Url)
            .item6Url(item6Url)
            .item0Name(item0Name)
            .item1Name(item1Name)
            .item2Name(item2Name)
            .item3Name(item3Name)
            .item4Name(item4Name)
            .item5Name(item5Name)
            .item6Name(item6Name)
            .item0Description(item0Description)
            .item1Description(item1Description)
            .item2Description(item2Description)
            .item3Description(item3Description)
            .item4Description(item4Description)
            .item5Description(item5Description)
            .item6Description(item6Description)
            .primaryRuneUrl(primaryRuneUrl)
            .primaryRuneName(primaryRuneName)
            .primaryRuneDescription(primaryRuneDescription)
            .subRuneUrl(subRuneUrl)
            .subRuneName(subRuneName)
            .subRuneDescription(subRuneDescription)
            .wardsPlaced(wardsPlaced)
            .teamId(teamId)
            .win(win)
            .build();

        if (teamId == BLUE_TEAM) {
          blueTeamSummoners.add(customGameSummonerDto);
        } else if (teamId == RED_TEAM) {
          redTeamSummoners.add(customGameSummonerDto);
        }
      }

      List<LolienTeamStats> teams = lolienMatch
          .getTeams()
          .stream()
          .sorted(Comparator.comparing(LolienTeamStats::getIdx))
          .collect(Collectors.toList());

      for (LolienTeamStats team : teams) {
        List<LolienTeamBans> bans = team.getBans();

        for (LolienTeamBans ban : bans) {
          int pickTurn = ban.getPickTurn();
          int championId = ban.getChampionId();

          CustomGameTeamBanDto teamBanDto = CustomGameTeamBanDto
              .builder()
              .pickTurn(pickTurn)
              .championId(championId)
              .build();

          teamBanDtoList.add(teamBanDto);
        }

        CustomGameTeamDto teamDto = CustomGameTeamDto
            .builder()
            .bans(teamBanDtoList)
            .build();

        teamDtoList.add(teamDto);
      }

      CustomGameResponse customGameResponse = CustomGameResponse
          .builder()
          .idx(idx)
          .gameCreation(gameCreation)
          .gameDuration(gameDuration)
          .gameId(gameId)
          .gameMode(gameMode)
          .gameType(gameType)
          .gameVersion(gameVersion)
          .mapId(mapId)
          .platformId(platformId)
          .queueId(queueId)
          .seasonId(seasonId)
          .blueTeamSummoners(blueTeamSummoners)
          .redTeamSummoners(redTeamSummoners)
          .teams(teamDtoList)
          .build();

      customGames.add(customGameResponse);
    }

    return CustomGamesResponse
        .builder()
        .customGames(customGames)
        .totalPages(totalPages)
        .build();
  }

  @Override
  public CustomGamesStatisticsResponse getStatistics() {
    List<LolienMatch> lolienMatches = getLolienMatches();
    List<ChampDto> championNames = riotComponent.getChampionNames();
    JsonObject championJsonObject = riotComponent.getChampionJsonObject();

    List<CustomGamesStatisticsMatchDto> matchesDto = getStatisticsMatchesDto(
        lolienMatches);
    List<CustomGamesStatisticsMostBannedDto> mostBannedDtoList = getStatisticsMostBannedDto(
        lolienMatches, championNames, championJsonObject);

    List<CustomGamesStatisticsMostPlayedChampionDto> mostPlayedChampionDtoList =
        getStatisticsMostPlayedChampionDto(lolienMatches, championNames, championJsonObject);

    List<CustomGamesStatisticsMostWinningDto> mostWinningDtoList = getStatisticsMostWinningDto(
        lolienMatches, championNames, championJsonObject);

    List<CustomGamesStatisticsMostPlayedSummonerDto> mostPlayedSummonerDtoList =
        getStatisticsMostPlayedSummonerDto(lolienMatches);

    List<CustomGamesStatisticsMostKillDeathAssistDto> mostKillDeathAssistDtoList =
        getMostKillDeathAssistDtoList(lolienMatches);

    CustomGamesStatisticsMostKillDto mostKillDto = getMostKillDto(lolienMatches);
    CustomGamesStatisticsMostDeathDto mostDeathDto = getMostDeathDto(lolienMatches);
    CustomGamesStatisticsMostAssistDto mostAssistDto = getMostAssistDto(lolienMatches);
    CustomGamesStatisticsMostVisionScoreDto mostVisionScoreDto = getMostVisionScoreDto(
        lolienMatches);

    return CustomGamesStatisticsResponse
        .builder()
        .matches(matchesDto)
        .mostBannedList(mostBannedDtoList)
        .mostPlayedChampionList(mostPlayedChampionDtoList)
        .mostWinningList(mostWinningDtoList)
        .mostPlayedSummonerList(mostPlayedSummonerDtoList)
        .mostKillDeathAssistList(mostKillDeathAssistDtoList)
        .mostKill(mostKillDto)
        .mostDeath(mostDeathDto)
        .mostAssist(mostAssistDto)
        .mostVisionScore(mostVisionScoreDto)
        .build();
  }

  private List<CustomGamesStatisticsMatchDto> getStatisticsMatchesDto(
      List<LolienMatch> lolienMatches) {

    return lolienMatches
        .stream()
        .collect(Collectors
            .groupingBy(lm -> timestampToLocalDateTime(lm.getGameCreation()).toLocalDate()))
        .entrySet()
        .stream()
        .map(entry -> CustomGamesStatisticsMatchDto
            .builder()
            .gameCreation(entry.getKey())
            .matchCount(entry.getValue().size())
            .build())
        .sorted(Comparator.comparing(CustomGamesStatisticsMatchDto::getGameCreation))
        .collect(Collectors.toList());
  }

  private List<LolienMatch> getLolienMatches() {
    LocalDate startDateOfMonth = getStartDateOfMonth();
    long startTimestamp = localDateToTimestamp(startDateOfMonth);

    LocalDate endDateOfMonth = getEndDateOfMonth();
    long endTimestamp = localDateToTimestamp(endDateOfMonth);

    return lolienMatchRepository
        .findByGameCreationGreaterThanEqualAndGameCreationLessThanEqual(startTimestamp,
            endTimestamp);
  }

  private List<CustomGamesStatisticsMostBannedDto> getStatisticsMostBannedDto(
      List<LolienMatch> lolienMatches, List<ChampDto> championNames,
      JsonObject championJsonObject) {

    List<CustomGamesStatisticsMostBannedDto> mostBannedDtoList = Lists.newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienTeamStats> teams = lolienMatch.getTeams();

      for (LolienTeamStats team : teams) {
        List<LolienTeamBans> bans = team.getBans();

        for (LolienTeamBans ban : bans) {
          int championId =  ban.getChampionId();
          String championName = riotComponent.getChampionNameByChampId(championNames, championId);
          String championUrl = riotComponent.getChampionUrl(championJsonObject, championId);

          CustomGamesStatisticsMostBannedDto mostBannedDto = mostBannedDtoList
              .stream()
              .filter(mb -> mb.getChampionName().equals(championName))
              .findFirst()
              .orElse(null);

          if (Objects.isNull(mostBannedDto)) {
            mostBannedDto = CustomGamesStatisticsMostBannedDto
                .builder()
                .championName(championName)
                .championUrl(championUrl)
                .count(1)
                .build();

            mostBannedDtoList.add(mostBannedDto);
          } else {
            mostBannedDto.increaseCount();
          }
        }
      }
    }

    return mostBannedDtoList
        .stream()
        .sorted(Comparator.comparing(CustomGamesStatisticsMostBannedDto::getCount).reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  private List<CustomGamesStatisticsMostPlayedChampionDto> getStatisticsMostPlayedChampionDto(
      List<LolienMatch> lolienMatches, List<ChampDto> championNames,
      JsonObject championJsonObject) {

    List<CustomGamesStatisticsMostPlayedChampionDto> mostPlayedChampionDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        int championId =  participant.getChampionId();
        String championName = riotComponent.getChampionNameByChampId(championNames, championId);
        String championUrl = riotComponent.getChampionUrl(championJsonObject, championId);

        CustomGamesStatisticsMostPlayedChampionDto mostPlayedChampionDto = mostPlayedChampionDtoList
            .stream()
            .filter(mb -> mb.getChampionName().equals(championName))
            .findFirst()
            .orElse(null);

        if (Objects.isNull(mostPlayedChampionDto)) {
          mostPlayedChampionDto = CustomGamesStatisticsMostPlayedChampionDto
              .builder()
              .championName(championName)
              .championUrl(championUrl)
              .count(1)
              .build();

          mostPlayedChampionDtoList.add(mostPlayedChampionDto);
        } else {
          mostPlayedChampionDto.increaseCount();
        }
      }
    }

    return mostPlayedChampionDtoList
        .stream()
        .sorted(Comparator.comparing(CustomGamesStatisticsMostPlayedChampionDto::getCount)
            .reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  private List<CustomGamesStatisticsMostWinningDto> getStatisticsMostWinningDto(
      List<LolienMatch> lolienMatches, List<ChampDto> championNames,
      JsonObject championJsonObject) {

    List<CustomGamesStatisticsMostWinningChampionDto> mostWinningChampionDtoList =
        getMostWinningChampionDtoList(lolienMatches);

    Map<Integer, List<CustomGamesStatisticsMostWinningChampionDto>> groupingBy =
        mostWinningChampionDtoList
        .stream()
        .collect(Collectors
            .groupingBy(CustomGamesStatisticsMostWinningChampionDto::getChampionId));

    List<CustomGamesStatisticsMostWinningDto> mostWinningDtoList = Lists.newArrayList();

    for (Map.Entry<Integer, List<CustomGamesStatisticsMostWinningChampionDto>> entry : groupingBy
        .entrySet()) {
      int championId = entry.getKey();
      String championName = riotComponent.getChampionNameByChampId(championNames, championId);
      String championUrl = riotComponent.getChampionUrl(championJsonObject, championId);

      List<CustomGamesStatisticsMostWinningChampionDto> championDtoList = entry.getValue();

      long totalPlayedCount = championDtoList.size();
      long winCount = championDtoList
          .stream()
          .filter(CustomGamesStatisticsMostWinningChampionDto::isWin)
          .count();
      float winRate = getWinRate(totalPlayedCount, winCount);

      CustomGamesStatisticsMostWinningDto mostWinningDto = CustomGamesStatisticsMostWinningDto
          .builder()
          .championName(championName)
          .championUrl(championUrl)
          .winRate(winRate)
          .totalPlayedCount(totalPlayedCount)
          .build();

      mostWinningDtoList.add(mostWinningDto);
    }

    Comparator<CustomGamesStatisticsMostWinningDto> compareCondition = Comparator
        .comparing(CustomGamesStatisticsMostWinningDto::getWinRate, reverseOrder())
        .thenComparing(CustomGamesStatisticsMostWinningDto::getTotalPlayedCount, reverseOrder());

    return mostWinningDtoList
        .stream()
        .sorted(compareCondition)
        .limit(3)
        .collect(Collectors.toList());
  }

  private float getWinRate(long totalPlayedCount, float winCount) {
    return (float) Math.floor((winCount / totalPlayedCount) * 100 * 100.0)
            / 100.0f;
  }

  /**
   * 챔피언별로 승패여부를 조회 (승률을 계산하려면 몇판중에 몇판 이겼는지를 알아야 함).
   * @param lolienMatches lolienMatches
   * @return 챔피언별 승패여부
   */
  private List<CustomGamesStatisticsMostWinningChampionDto> getMostWinningChampionDtoList(
      List<LolienMatch> lolienMatches) {

    List<CustomGamesStatisticsMostWinningChampionDto> mostWinningChampionDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        int championId =  participant.getChampionId();
        LolienParticipantStats stats = participant.getStats();
        boolean win = stats.getWin();

        CustomGamesStatisticsMostWinningChampionDto mostWinningChampionDto =
            CustomGamesStatisticsMostWinningChampionDto
            .builder()
            .championId(championId)
            .win(win)
            .build();

        mostWinningChampionDtoList.add(mostWinningChampionDto);
      }
    }
    return mostWinningChampionDtoList;
  }

  private List<CustomGamesStatisticsMostPlayedSummonerDto> getStatisticsMostPlayedSummonerDto(
      List<LolienMatch> lolienMatches) {

    List<CustomGamesStatisticsMostPlayedSummonerDto> mostPlayedSummonerDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();

        CustomGamesStatisticsMostPlayedSummonerDto mostPlayedSummonerDto = mostPlayedSummonerDtoList
            .stream()
            .filter(mb -> mb.getSummonerName().equals(summonerName))
            .findFirst()
            .orElse(null);

        if (Objects.isNull(mostPlayedSummonerDto)) {
          mostPlayedSummonerDto = CustomGamesStatisticsMostPlayedSummonerDto
              .builder()
              .summonerName(summonerName)
              .count(1)
              .build();

          mostPlayedSummonerDtoList.add(mostPlayedSummonerDto);
        } else {
          mostPlayedSummonerDto.increaseCount();
        }
      }
    }

    return mostPlayedSummonerDtoList
        .stream()
        .sorted(Comparator.comparing(CustomGamesStatisticsMostPlayedSummonerDto::getCount)
            .reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  /**
   * 소환사별 KDA 조회.
   * @param lolienMatches lolienMatches
   * @return 소환사별 KDA
   */
  private List<CustomGamesStatisticsMostKillDeathAssistDto> getMostKillDeathAssistDtoList(
      List<LolienMatch> lolienMatches) {

    List<CustomGamesStatisticsMostKillDeathAssistDto> mostKillDeathAssistDtoList = Lists
        .newArrayList();

    List<CustomGamesStatisticsMostKillDeathAssistInfoDto> mostKillDeathAssistInfoDtoList =
        getMostKillDeathAssistInfoDtoList(lolienMatches);

    for (CustomGamesStatisticsMostKillDeathAssistInfoDto mostKillDeathAssistInfoDto
        : mostKillDeathAssistInfoDtoList) {
      String summonerName = mostKillDeathAssistInfoDto.getSummonerName();
      int kills = mostKillDeathAssistInfoDto.getKills();
      int deaths = mostKillDeathAssistInfoDto.getDeaths();
      int assists = mostKillDeathAssistInfoDto.getAssists();
      float kda = getKda(kills, deaths, assists);

      CustomGamesStatisticsMostKillDeathAssistDto mostKillDeathAssistDto =
          CustomGamesStatisticsMostKillDeathAssistDto
              .builder()
              .summonerName(summonerName)
              .kda(kda)
              .build();

      mostKillDeathAssistDtoList.add(mostKillDeathAssistDto);
    }

    return mostKillDeathAssistDtoList
        .stream()
        .sorted(Comparator.comparing(CustomGamesStatisticsMostKillDeathAssistDto::getKda)
            .reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  /**
   * 소환사별 KDA를 구하기 위해서 소환사의 모든 내전의 K, D, A를 구해서 더함.
   * @param lolienMatches lolienMatches
   * @return 모든 내전의 K, D, A 합
   */
  private List<CustomGamesStatisticsMostKillDeathAssistInfoDto> getMostKillDeathAssistInfoDtoList(
      List<LolienMatch> lolienMatches) {

    List<CustomGamesStatisticsMostKillDeathAssistInfoDto> mostKillDeathAssistInfoDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();

        LolienParticipantStats stats = participant.getStats();
        int kills = stats.getKills();
        int deaths = stats.getDeaths();
        int assists = stats.getAssists();

        CustomGamesStatisticsMostKillDeathAssistInfoDto mostKillDeathAssistsInfoDto =
            mostKillDeathAssistInfoDtoList
                .stream()
                .filter(mb -> mb.getSummonerName().equals(summonerName))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(mostKillDeathAssistsInfoDto)) {
          mostKillDeathAssistsInfoDto = CustomGamesStatisticsMostKillDeathAssistInfoDto
              .builder()
              .summonerName(summonerName)
              .kills(kills)
              .deaths(deaths)
              .assists(assists)
              .build();

          mostKillDeathAssistInfoDtoList.add(mostKillDeathAssistsInfoDto);
        } else {
          mostKillDeathAssistsInfoDto.plusKills(kills);
          mostKillDeathAssistsInfoDto.plusDeaths(deaths);
          mostKillDeathAssistsInfoDto.plusAssists(assists);
        }
      }
    }

    return mostKillDeathAssistInfoDtoList;
  }

  private float getKda(int kills, int deaths, int assists) {
    if (deaths == 0) {
      return kills + assists;
    }

    return (float) Math.floor((float) (kills + assists) / deaths * 100.0) / 100.0f;
  }

  /**
   * 가장 많이 처치한 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 가장 많이 처치한 소환사
   */
  private CustomGamesStatisticsMostKillDto getMostKillDto(List<LolienMatch> lolienMatches) {
    long gameId = 0;
    String summonerName = "";
    int mostKills = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        int kills = stats.getKills();

        if (mostKills < kills) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostKills = kills;
        }
      }
    }

    return CustomGamesStatisticsMostKillDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .kills(mostKills)
        .build();
  }

  /**
   * 가장 많이 죽은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 가장 많이 죽은 소환사
   */
  private CustomGamesStatisticsMostDeathDto getMostDeathDto(List<LolienMatch> lolienMatches) {
    long gameId = 0;
    String summonerName = "";
    int mostDeaths = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        int deaths = stats.getDeaths();

        if (mostDeaths < deaths) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostDeaths = deaths;
        }
      }
    }

    return CustomGamesStatisticsMostDeathDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .deaths(mostDeaths)
        .build();
  }

  /**
   * 가장 많이 처치 기여한 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 가장 많이 처치 기여한 소환사
   */
  private CustomGamesStatisticsMostAssistDto getMostAssistDto(List<LolienMatch> lolienMatches) {
    long gameId = 0;
    String summonerName = "";
    int mostAssists = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        int assists = stats.getAssists();

        if (mostAssists < assists) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostAssists = assists;
        }
      }
    }

    return CustomGamesStatisticsMostAssistDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .assists(mostAssists)
        .build();
  }

  /**
   * 시야 점수가 가장 높은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 가장 많이 처치 기여한 소환사
   */
  private CustomGamesStatisticsMostVisionScoreDto getMostVisionScoreDto(
      List<LolienMatch> lolienMatches) {

    long gameId = 0;
    String summonerName = "";
    long mostVisionScore = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        long visionScore = stats.getVisionScore();

        if (mostVisionScore < visionScore) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostVisionScore = visionScore;
        }
      }
    }

    return CustomGamesStatisticsMostVisionScoreDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .visionScore(mostVisionScore)
        .build();
  }
}
