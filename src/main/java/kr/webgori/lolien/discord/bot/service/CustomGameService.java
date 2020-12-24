package kr.webgori.lolien.discord.bot.service;

import static java.util.Collections.reverseOrder;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.DEFAULT_CHARSET;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getEndDateOfMonth;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getEndDateOfPrevMonth;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getStartDateOfMonth;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getStartDateOfPrevMonth;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.localDateTimeToTimestamp;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.localDateToTimestamp;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.timestampToLocalDateTime;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.component.RiotComponent;
import kr.webgori.lolien.discord.bot.dto.ChampDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamBanDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamDto;
import kr.webgori.lolien.discord.bot.dto.DataDragonVersionDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MatchDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MmrDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostAssistDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostBannedDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostDeathDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostFirstBloodKillDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostFirstTowerKillDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostGoldEarnedDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostKillDeathAssistDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostKillDeathAssistInfoDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostKillDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostMinionsKilledDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostPlayedChampionDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostPlayedSummonerDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostTotalDamageDealtToChampionsDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostTotalDamageTakenDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostVisionScoreDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostWinningChampionDto;
import kr.webgori.lolien.discord.bot.dto.customgame.statistics.MostWinningDto;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienParticipantStats;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienTeamBans;
import kr.webgori.lolien.discord.bot.entity.LolienTeamStats;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.exception.SummonerNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LolienMatchRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.response.CustomGameDto;
import kr.webgori.lolien.discord.bot.response.CustomGamesResponse;
import kr.webgori.lolien.discord.bot.response.StatisticsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomGameService {
  static final int BLUE_TEAM = 100;
  static final int RED_TEAM = 200;

  private final LolienMatchRepository lolienMatchRepository;
  private final CustomGameComponent customGameComponent;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final RiotComponent riotComponent;
  private final AuthenticationComponent authenticationComponent;
  private final HttpServletRequest httpServletRequest;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;

  /**
   * addResult.
   * @param customGameAddResultRequest customGameAddResultRequest
   */
  @Transactional
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

  /**
   * getCustomGames.
   * @param page page
   * @param size size
   * @return CustomGamesResponse
   */
  @Transactional(readOnly = true)
  public CustomGamesResponse getCustomGames(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);

    Page<LolienMatch> lolienMatchePages = lolienMatchRepository
        .findByOrderByGameCreationDesc(pageRequest);
    List<LolienMatch> lolienMatches = lolienMatchePages.getContent();

    int totalPages = lolienMatchePages.getTotalPages();

    return getCustomGamesResponse(lolienMatches, totalPages);
  }

  /**
   * getCustomGamesBySummoner.
   * @param targetSummonerName targetSummonerName
   * @param page page
   * @param  size size
   * @return CustomGamesResponse
   */
  @Transactional(readOnly = true)
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

    List<CustomGameDto> customGamesDto = Lists.newArrayList();
    List<CustomGameTeamDto> teamDtoList = Lists.newArrayList();
    List<CustomGameTeamBanDto> teamBanDtoList = Lists.newArrayList();

    List<DataDragonVersionDto> dataDragonVersions = riotComponent.getDataDragonVersions();
    Map<String, JsonObject> summonerJsonObjectMap = Maps.newHashMap();
    Map<String, JsonObject> championsJsonObjectMap = Maps.newHashMap();
    Map<String, JsonObject> itemsJsonObjectMap = Maps.newHashMap();
    Map<String, JsonArray> runesJsonArrayMap = Maps.newHashMap();

    User user = null;

    try {
      user = authenticationComponent.getUser(httpServletRequest);
    } catch (ExpiredJwtException | BadCredentialsException | MalformedJwtException e) {
      logger.error("", e);
    }

    for (LolienMatch lolienMatch : lolienMatches) {
      String gameVersion = lolienMatch.getGameVersion();

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

      User matchUser = lolienMatch.getUser();
      boolean deleteAble = false;

      if (Objects.nonNull(user) && matchUser.equals(user)) {
        deleteAble = true;
      }

      int idx = lolienMatch.getIdx();
      long gameCreation = lolienMatch.getGameCreation();
      long gameDuration = lolienMatch.getGameDuration();
      long gameId = lolienMatch.getGameId();
      String gameMode = lolienMatch.getGameMode();
      String gameType = lolienMatch.getGameType();
      int mapId = lolienMatch.getMapId();
      String platformId = lolienMatch.getPlatformId();
      int queueId = lolienMatch.getQueueId();
      int seasonId = lolienMatch.getSeasonId();

      CustomGameDto customGameDto = CustomGameDto
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
          .deleteAble(deleteAble)
          .build();

      customGamesDto.add(customGameDto);
    }

    return CustomGamesResponse
        .builder()
        .customGames(customGamesDto)
        .totalPages(totalPages)
        .build();
  }

  /**
   * getStatistics.
   * @return CustomGamesStatisticsResponse
   */
  public StatisticsResponse getStatistics() {
    LocalDate startDateOfMonth = getStartDateOfMonth();
    LocalDate endDateOfMonth = getEndDateOfMonth();

    Optional<StatisticsResponse> statisticsFromCache = getStatisticsFromCache(startDateOfMonth,
        endDateOfMonth);

    StatisticsResponse statisticsResponse = statisticsFromCache
            .orElseGet(() -> StatisticsResponse.builder().build());

    List<MatchDto> matches = statisticsResponse.getMatches();

    if (!matches.isEmpty()) {
      return statisticsResponse;
    }

    List<LolienMatch> lolienMatches = getLolienMatches(startDateOfMonth, endDateOfMonth);

    if (lolienMatches.isEmpty()) {
      startDateOfMonth = getStartDateOfPrevMonth();
      endDateOfMonth = getEndDateOfPrevMonth();

      statisticsFromCache = getStatisticsFromCache(startDateOfMonth, endDateOfMonth);

      statisticsResponse = statisticsFromCache
          .orElseGet(() -> StatisticsResponse.builder().build());

      matches = statisticsResponse.getMatches();

      if (!matches.isEmpty()) {
        return statisticsResponse;
      }

      lolienMatches = getLolienMatches(startDateOfMonth, endDateOfMonth);
    }

    List<ChampDto> championNames = riotComponent.getChampionNames();
    JsonObject championJsonObject = riotComponent.getChampionJsonObject();

    List<MatchDto> matchesDto = getStatisticsMatchesDto(
        lolienMatches);
    List<MostBannedDto> mostBannedDtoList = getStatisticsMostBannedDto(
        lolienMatches, championNames, championJsonObject);

    List<MostPlayedChampionDto> mostPlayedChampionDtoList =
        getStatisticsMostPlayedChampionDto(lolienMatches, championNames, championJsonObject);

    List<MostWinningDto> mostWinningDtoList = getStatisticsMostWinningDto(
        lolienMatches, championNames, championJsonObject);

    List<MostPlayedSummonerDto> mostPlayedSummonerDtoList =
        getStatisticsMostPlayedSummonerDto(lolienMatches);

    List<MostKillDeathAssistDto> mostKillDeathAssistDtoList =
        getMostKillDeathAssistDtoList(lolienMatches);

    MostKillDto mostKillDto = getMostKillDto(lolienMatches);
    MostDeathDto mostDeathDto = getMostDeathDto(lolienMatches);
    MostAssistDto mostAssistDto = getMostAssistDto(lolienMatches);
    MostVisionScoreDto mostVisionScoreDto = getMostVisionScoreDto(lolienMatches);
    MostTotalDamageDealtToChampionsDto mostTotalDamageDealtToChampions =
        getMostTotalDamageDealtToChampions(lolienMatches);

    MostTotalDamageTakenDto mostTotalDamageTakenDto = getMostTotalDamageTakenDto(lolienMatches);
    MostGoldEarnedDto mostGoldEarnedDto = getMostGoldEarnedDto(lolienMatches);
    MostMinionsKilledDto mostMinionsKilledDto = getMostMinionsKilledDto(lolienMatches);
    MostFirstTowerKillDto mostFirstTowerKillDto = getMostFirstTowerKillDto(lolienMatches);
    MostFirstBloodKillDto mostFirstBloodKillDto = getMostFirstBloodKillDto(lolienMatches);
    MmrDto minMmrDto = getMinMmr();
    MmrDto maxMmrDto = getMaxMmr();

    statisticsResponse = StatisticsResponse
        .builder()
        .startDateOfMonth(startDateOfMonth)
        .endDateOfMonth(endDateOfMonth)
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
        .mostTotalDamageDealtToChampions(mostTotalDamageDealtToChampions)
        .mostTotalDamageTaken(mostTotalDamageTakenDto)
        .mostGoldEarned(mostGoldEarnedDto)
        .mostNeutralMinionsKilled(mostMinionsKilledDto)
        .mostFirstTowerKill(mostFirstTowerKillDto)
        .mostFirstBloodKill(mostFirstBloodKillDto)
        .minMmr(minMmrDto)
        .maxMmr(maxMmrDto)
        .build();

    cachingStatistics(statisticsResponse);

    return statisticsResponse;
  }

  private void cachingStatistics(StatisticsResponse statisticsResponse) {
    String key = "lolien-discord-bot:custom-game:statistics-%s-%s";
    LocalDate startDateOfMonth = statisticsResponse.getStartDateOfMonth();
    LocalDate endDateOfMonth = statisticsResponse.getEndDateOfMonth();
    String redisKey = String.format(key, startDateOfMonth, endDateOfMonth);

    redisTemplate.opsForValue().set(redisKey, statisticsResponse);
  }

  private MmrDto getMaxMmr() {
    List<LolienSummoner> lolienSummoners = lolienSummonerRepository
            .findByMmrNotNullOrderByMmrDesc();

    for (LolienSummoner lolienSummoner : lolienSummoners) {
      Set<LolienParticipant> participants = lolienSummoner.getParticipants();

      if (!participants.isEmpty()) {
        LolienMatch lolienMatch = participants
            .stream()
            .map(LolienParticipant::getMatch)
            .max(Comparator.comparing(LolienMatch::getGameCreation))
            .orElseThrow(() -> new IllegalArgumentException(""));

        if (isGameCreationBeforeThreeMonth(lolienMatch)) {
          continue;
        }

        String summonerName = lolienSummoner.getSummonerName();
        int mmr = lolienSummoner.getMmr();

        return MmrDto
                .builder()
                .summonerName(summonerName)
                .mmr(mmr)
                .build();
      }
    }

    return MmrDto
            .builder()
            .summonerName("")
            .mmr(0)
            .build();
  }

  private MmrDto getMinMmr() {
    List<LolienSummoner> lolienSummoners = lolienSummonerRepository
            .findByMmrNotNullOrderByMmrAsc();

    for (LolienSummoner lolienSummoner : lolienSummoners) {
      Set<LolienParticipant> participants = lolienSummoner.getParticipants();

      if (!participants.isEmpty()) {
        LolienMatch lolienMatch = participants
            .stream()
            .map(LolienParticipant::getMatch)
            .max(Comparator.comparing(LolienMatch::getGameCreation))
            .orElseThrow(() -> new IllegalArgumentException(""));

        if (isGameCreationBeforeThreeMonth(lolienMatch)) {
          continue;
        }

        String summonerName = lolienSummoner.getSummonerName();
        int mmr = lolienSummoner.getMmr();

        return MmrDto
                .builder()
                .summonerName(summonerName)
                .mmr(mmr)
                .build();
      }
    }

    return MmrDto
            .builder()
            .summonerName("")
            .mmr(0)
            .build();
  }

  private boolean isGameCreationBeforeThreeMonth(LolienMatch lolienMatch) {
    long gameCreation = lolienMatch.getGameCreation();

    LocalDateTime threeMonthAgoDateTime = LocalDateTime.now().minusMonths(3);
    long threeMonthAgoTimestamp = localDateTimeToTimestamp(threeMonthAgoDateTime);

    return threeMonthAgoTimestamp >= gameCreation;
  }

  private List<MatchDto> getStatisticsMatchesDto(List<LolienMatch> lolienMatches) {
    return lolienMatches
        .stream()
        .collect(Collectors
            .groupingBy(lm -> timestampToLocalDateTime(lm.getGameCreation()).toLocalDate()))
        .entrySet()
        .stream()
        .map(entry -> MatchDto
            .builder()
            .gameCreation(entry.getKey())
            .matchCount(entry.getValue().size())
            .build())
        .sorted(Comparator.comparing(MatchDto::getGameCreation))
        .collect(Collectors.toList());
  }

  /**
   * getLolienMatches.
   * @param startDateOfMonth startDateOfMonth
   * @param endDateOfMonth endDateOfMonth
   * @return lolienMatches
   */
  Optional<StatisticsResponse> getStatisticsFromCache(LocalDate startDateOfMonth,
                                                      LocalDate endDateOfMonth) {

    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();

    String key = "lolien-discord-bot:custom-game:statistics-%s-%s";
    String redisKey = String.format(key, startDateOfMonth, endDateOfMonth);

    boolean hasKey = Optional
        .ofNullable(opsForValue.getOperations().hasKey(redisKey))
        .orElse(false);

    if (hasKey) {
      Object obj = redisTemplate.opsForValue().get(redisKey);
      return Optional.ofNullable(objectMapper.convertValue(obj, StatisticsResponse.class));
    } else {
      return Optional.empty();
    }
  }

  /**
   * getLolienMatches.
   * @param startDateOfMonth startDateOfMonth
   * @param endDateOfMonth endDateOfMonth
   * @return lolienMatches
   */
  List<LolienMatch> getLolienMatches(LocalDate startDateOfMonth, LocalDate endDateOfMonth) {
    long startTimestamp = localDateToTimestamp(startDateOfMonth);
    long endTimestamp = localDateToTimestamp(endDateOfMonth);

    return lolienMatchRepository
        .findByGameCreationGreaterThanEqualAndGameCreationLessThanEqual(startTimestamp,
            endTimestamp);
  }

  private List<MostBannedDto> getStatisticsMostBannedDto(List<LolienMatch> lolienMatches,
                                                         List<ChampDto> championNames,
                                                         JsonObject championJsonObject) {

    List<MostBannedDto> mostBannedDtoList = Lists.newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienTeamStats> teams = lolienMatch.getTeams();

      for (LolienTeamStats team : teams) {
        List<LolienTeamBans> bans = team.getBans();

        for (LolienTeamBans ban : bans) {
          int championId =  ban.getChampionId();
          String championName = riotComponent.getChampionNameByChampId(championNames, championId);
          String championUrl = riotComponent.getChampionUrl(championJsonObject, championId);

          MostBannedDto mostBannedDto = mostBannedDtoList
              .stream()
              .filter(mb -> mb.getChampionName().equals(championName))
              .findFirst()
              .orElse(null);

          if (Objects.isNull(mostBannedDto)) {
            mostBannedDto = MostBannedDto
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
        .sorted(Comparator.comparing(MostBannedDto::getCount).reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  private List<MostPlayedChampionDto> getStatisticsMostPlayedChampionDto(
      List<LolienMatch> lolienMatches, List<ChampDto> championNames,
      JsonObject championJsonObject) {

    List<MostPlayedChampionDto> mostPlayedChampionDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        int championId =  participant.getChampionId();
        String championName = riotComponent.getChampionNameByChampId(championNames, championId);
        String championUrl = riotComponent.getChampionUrl(championJsonObject, championId);

        MostPlayedChampionDto mostPlayedChampionDto = mostPlayedChampionDtoList
            .stream()
            .filter(mb -> mb.getChampionName().equals(championName))
            .findFirst()
            .orElse(null);

        if (Objects.isNull(mostPlayedChampionDto)) {
          mostPlayedChampionDto = MostPlayedChampionDto
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
        .sorted(Comparator.comparing(MostPlayedChampionDto::getCount)
            .reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  private List<MostWinningDto> getStatisticsMostWinningDto(List<LolienMatch> lolienMatches,
                                                           List<ChampDto> championNames,
                                                           JsonObject championJsonObject) {

    List<MostWinningChampionDto> mostWinningChampionDtoList =
        getMostWinningChampionDtoList(lolienMatches);

    Map<Integer, List<MostWinningChampionDto>> groupingBy =
        mostWinningChampionDtoList
        .stream()
        .collect(Collectors
            .groupingBy(MostWinningChampionDto::getChampionId));

    List<MostWinningDto> mostWinningDtoList = Lists.newArrayList();

    for (Map.Entry<Integer, List<MostWinningChampionDto>> entry : groupingBy
        .entrySet()) {
      int championId = entry.getKey();
      String championName = riotComponent.getChampionNameByChampId(championNames, championId);
      String championUrl = riotComponent.getChampionUrl(championJsonObject, championId);

      List<MostWinningChampionDto> championDtoList = entry.getValue();

      long totalPlayedCount = championDtoList.size();
      long winCount = championDtoList
          .stream()
          .filter(MostWinningChampionDto::isWin)
          .count();
      float winRate = getWinRate(totalPlayedCount, winCount);

      MostWinningDto mostWinningDto = MostWinningDto
          .builder()
          .championName(championName)
          .championUrl(championUrl)
          .winRate(winRate)
          .totalPlayedCount(totalPlayedCount)
          .build();

      mostWinningDtoList.add(mostWinningDto);
    }

    Comparator<MostWinningDto> compareCondition = Comparator
        .comparing(MostWinningDto::getWinRate, reverseOrder())
        .thenComparing(MostWinningDto::getTotalPlayedCount, reverseOrder());

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
  private List<MostWinningChampionDto> getMostWinningChampionDtoList(
      List<LolienMatch> lolienMatches) {

    List<MostWinningChampionDto> mostWinningChampionDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        int championId =  participant.getChampionId();
        LolienParticipantStats stats = participant.getStats();
        boolean win = stats.getWin();

        MostWinningChampionDto mostWinningChampionDto =
            MostWinningChampionDto
            .builder()
            .championId(championId)
            .win(win)
            .build();

        mostWinningChampionDtoList.add(mostWinningChampionDto);
      }
    }
    return mostWinningChampionDtoList;
  }

  private List<MostPlayedSummonerDto> getStatisticsMostPlayedSummonerDto(
      List<LolienMatch> lolienMatches) {

    List<MostPlayedSummonerDto> mostPlayedSummonerDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();

        MostPlayedSummonerDto mostPlayedSummonerDto = mostPlayedSummonerDtoList
            .stream()
            .filter(mb -> mb.getSummonerName().equals(summonerName))
            .findFirst()
            .orElse(null);

        if (Objects.isNull(mostPlayedSummonerDto)) {
          mostPlayedSummonerDto = MostPlayedSummonerDto
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
        .sorted(Comparator.comparing(MostPlayedSummonerDto::getCount)
            .reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  /**
   * 소환사별 KDA 조회.
   * @param lolienMatches lolienMatches
   * @return 소환사별 KDA
   */
  private List<MostKillDeathAssistDto> getMostKillDeathAssistDtoList(
      List<LolienMatch> lolienMatches) {

    List<MostKillDeathAssistDto> mostKillDeathAssistDtoList = Lists
        .newArrayList();

    List<MostKillDeathAssistInfoDto> mostKillDeathAssistInfoDtoList =
        getMostKillDeathAssistInfoDtoList(lolienMatches);

    for (MostKillDeathAssistInfoDto mostKillDeathAssistInfoDto
        : mostKillDeathAssistInfoDtoList) {
      String summonerName = mostKillDeathAssistInfoDto.getSummonerName();
      int kills = mostKillDeathAssistInfoDto.getKills();
      int deaths = mostKillDeathAssistInfoDto.getDeaths();
      int assists = mostKillDeathAssistInfoDto.getAssists();
      float kda = getKda(kills, deaths, assists);

      MostKillDeathAssistDto mostKillDeathAssistDto =
          MostKillDeathAssistDto
              .builder()
              .summonerName(summonerName)
              .kda(kda)
              .build();

      mostKillDeathAssistDtoList.add(mostKillDeathAssistDto);
    }

    return mostKillDeathAssistDtoList
        .stream()
        .sorted(Comparator.comparing(MostKillDeathAssistDto::getKda)
            .reversed())
        .limit(3)
        .collect(Collectors.toList());
  }

  /**
   * 소환사별 KDA를 구하기 위해서 소환사의 모든 내전의 K, D, A를 구해서 더함.
   * @param lolienMatches lolienMatches
   * @return 모든 내전의 K, D, A 합
   */
  private List<MostKillDeathAssistInfoDto> getMostKillDeathAssistInfoDtoList(
      List<LolienMatch> lolienMatches) {

    List<MostKillDeathAssistInfoDto> mostKillDeathAssistInfoDtoList = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();

        LolienParticipantStats stats = participant.getStats();
        int kills = stats.getKills();
        int deaths = stats.getDeaths();
        int assists = stats.getAssists();

        MostKillDeathAssistInfoDto mostKillDeathAssistsInfoDto =
            mostKillDeathAssistInfoDtoList
                .stream()
                .filter(mb -> mb.getSummonerName().equals(summonerName))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(mostKillDeathAssistsInfoDto)) {
          mostKillDeathAssistsInfoDto = MostKillDeathAssistInfoDto
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
  private MostKillDto getMostKillDto(List<LolienMatch> lolienMatches) {
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

    return MostKillDto
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
  private MostDeathDto getMostDeathDto(List<LolienMatch> lolienMatches) {
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

    return MostDeathDto
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
  private MostAssistDto getMostAssistDto(List<LolienMatch> lolienMatches) {
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

    return MostAssistDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .assists(mostAssists)
        .build();
  }

  /**
   * 시야 점수가 가장 높은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 시야 점수가 가장 높은 소환사
   */
  private MostVisionScoreDto getMostVisionScoreDto(List<LolienMatch> lolienMatches) {
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

    return MostVisionScoreDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .visionScore(mostVisionScore)
        .build();
  }

  /**
   * 총 챔피언에게 가한 피해량이 가장 높은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 총 챔피언에게 가한 피해량이 가장 높은 소환사
   */
  private MostTotalDamageDealtToChampionsDto getMostTotalDamageDealtToChampions(
      List<LolienMatch> lolienMatches) {

    long gameId = 0;
    String summonerName = "";
    long mostTotalDamageDealtToChampions = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        long totalDamageDealtToChampions = stats.getTotalDamageDealtToChampions();

        if (mostTotalDamageDealtToChampions < totalDamageDealtToChampions) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostTotalDamageDealtToChampions = totalDamageDealtToChampions;
        }
      }
    }

    return MostTotalDamageDealtToChampionsDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .totalDamageDealtToChampions(mostTotalDamageDealtToChampions)
        .build();
  }

  /**
   * 총 받은 피해량이 가장 높은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 총 받은 피해량이 가장 높은 소환사
   */
  private MostTotalDamageTakenDto getMostTotalDamageTakenDto(List<LolienMatch> lolienMatches) {
    long gameId = 0;
    String summonerName = "";
    long mostTotalDamageTaken = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        long totalDamageTaken = stats.getTotalDamageTaken();

        if (mostTotalDamageTaken < totalDamageTaken) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostTotalDamageTaken = totalDamageTaken;
        }
      }
    }

    return MostTotalDamageTakenDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .totalDamageTaken(mostTotalDamageTaken)
        .build();
  }

  /**
   * 획득한 골드가 가장 높은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 획득한 골드가 가장 높은 소환사
   */
  private MostGoldEarnedDto getMostGoldEarnedDto(List<LolienMatch> lolienMatches) {
    long gameId = 0;
    String summonerName = "";
    int mostGoldEarned = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        int goldEarned = stats.getGoldEarned();

        if (mostGoldEarned < goldEarned) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostGoldEarned = goldEarned;
        }
      }
    }

    return MostGoldEarnedDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .goldEarned(mostGoldEarned)
        .build();
  }

  /**
   * CS가 가장 높은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return CS가 가장 높은 소환사
   */
  private MostMinionsKilledDto getMostMinionsKilledDto(List<LolienMatch> lolienMatches) {
    long gameId = 0;
    String summonerName = "";
    long mostMinionsKilled = 0;

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        LolienParticipantStats stats = participant.getStats();
        int totalMinionsKilled = stats.getTotalMinionsKilled();
        long neutralMinionsKilled = stats.getNeutralMinionsKilled();
        long minionsKilled = totalMinionsKilled + neutralMinionsKilled;

        if (mostMinionsKilled < minionsKilled) {
          gameId = lolienMatch.getGameId();
          summonerName = participant.getLolienSummoner().getSummonerName();
          mostMinionsKilled = minionsKilled;
        }
      }
    }

    return MostMinionsKilledDto
        .builder()
        .gameId(gameId)
        .summonerName(summonerName)
        .neutralMinionsKilled(mostMinionsKilled)
        .build();
  }

  /**
   * FIRST BLOOD가 가장 많은 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return FIRST BLOOD가 가장 많은 소환사
   */
  private MostFirstBloodKillDto getMostFirstBloodKillDto(List<LolienMatch> lolienMatches) {
    List<MostFirstBloodKillDto> mostFirstBloodKillDtoList = Lists.newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();
        LolienParticipantStats stats = participant.getStats();
        boolean firstBloodKill = stats.getFirstBloodKill();

        if (firstBloodKill) {
          MostFirstBloodKillDto mostFirstBloodKillDto = mostFirstBloodKillDtoList
              .stream()
              .filter(mb -> mb.getSummonerName().equals(summonerName))
              .findFirst()
              .orElse(null);

          if (Objects.isNull(mostFirstBloodKillDto)) {
            mostFirstBloodKillDto = MostFirstBloodKillDto
                .builder()
                .summonerName(summonerName)
                .count(1)
                .build();

            mostFirstBloodKillDtoList.add(mostFirstBloodKillDto);
          } else {
            mostFirstBloodKillDto.increaseCount();
          }
        }
      }
    }

    return mostFirstBloodKillDtoList
        .stream()
        .sorted(Comparator.comparing(MostFirstBloodKillDto::getCount)
            .reversed())
        .limit(1)
        .findFirst()
        .orElseGet(() -> MostFirstBloodKillDto
            .builder()
            .summonerName("")
            .count(0)
            .build());
  }

  /**
   * 첫 포탑을 가장 많이 파괴한 소환사 조회.
   * @param lolienMatches lolienMatches
   * @return 첫 포탑을 가장 많이 파괴한 소환사
   */
  private MostFirstTowerKillDto getMostFirstTowerKillDto(List<LolienMatch> lolienMatches) {
    List<MostFirstTowerKillDto> mostFirstTowerKillDtoList = Lists.newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();
        LolienParticipantStats stats = participant.getStats();
        boolean firstTowerKill = stats.getFirstTowerKill();

        if (firstTowerKill) {
          MostFirstTowerKillDto mostFirstTowerKillDto = mostFirstTowerKillDtoList
              .stream()
              .filter(mb -> mb.getSummonerName().equals(summonerName))
              .findFirst()
              .orElse(null);

          if (Objects.isNull(mostFirstTowerKillDto)) {
            mostFirstTowerKillDto = MostFirstTowerKillDto
                .builder()
                .summonerName(summonerName)
                .count(1)
                .build();

            mostFirstTowerKillDtoList.add(mostFirstTowerKillDto);
          } else {
            mostFirstTowerKillDto.increaseCount();
          }
        }
      }
    }

    return mostFirstTowerKillDtoList
        .stream()
        .sorted(Comparator.comparing(MostFirstTowerKillDto::getCount)
            .reversed())
        .limit(1)
        .findFirst()
        .orElseGet(() -> MostFirstTowerKillDto
            .builder()
            .summonerName("")
            .count(0)
            .build());
  }

  /**
   * 내전 결과 제거.
   * @param gameId gameId
   */
  @Transactional
  public void deleteResult(long gameId) {
    boolean existsByGameId = lolienMatchRepository.existsByGameId(gameId);

    if (!existsByGameId) {
      throw new IllegalArgumentException("내전 결과가 존재하지 않습니다.");
    }

    lolienMatchRepository.deleteByGameId(gameId);
  }

  /**
   * addLeagueResultByFiles.
   * @param files files
   */
  public void addResultByFiles(List<MultipartFile> files) {
    Pattern pattern = Pattern.compile("\\\\\"NAME\\\\\":\\\\\"([A-Za-z0-9가-힣 ]*)\\\\\"");

    for (MultipartFile file : files) {
      CustomGameAddResultRequest request = new CustomGameAddResultRequest();

      long gameId = getGameId(file);
      request.setMatchId(gameId);

      String entries = getEntries(file, pattern);
      request.setEntries(entries);

      addResult(request);
    }
  }

  private long getGameId(MultipartFile multipartFile) {
    String originalFilename = multipartFile.getOriginalFilename();
    originalFilename = FilenameUtils.removeExtension(originalFilename);

    if (Objects.isNull(originalFilename)) {
      throw new IllegalArgumentException("invalid league result file");
    }

    return Long.parseLong(originalFilename.replace("KR-", ""));
  }

  private String getEntries(MultipartFile file, Pattern pattern) {
    StringJoiner entryStringJoiner = new StringJoiner(",");

    try {
      String contents = new String(file.getBytes(), DEFAULT_CHARSET);
      Matcher matcher = pattern.matcher(contents);

      while (matcher.find()) {
        String summonerName = stripSummonerName(matcher.group());
        entryStringJoiner.add(summonerName);
      }
    } catch (IOException e) {
      logger.error("", e);
    }

    if (entryStringJoiner.toString().split(",").length != 10) {
      throw new IllegalArgumentException("invalid league result file");
    }

    return entryStringJoiner.toString();
  }

  private String stripSummonerName(String summonerName) {
    summonerName = summonerName.replace("NAME", "");
    summonerName = summonerName.replace("\"", "");
    summonerName = summonerName.replace("\\", "");
    summonerName = summonerName.replace(":", "");
    return summonerName.replaceAll("\\s+", "");
  }
}
