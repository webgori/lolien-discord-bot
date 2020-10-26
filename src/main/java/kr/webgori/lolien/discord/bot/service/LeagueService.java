package kr.webgori.lolien.discord.bot.service;

import static kr.webgori.lolien.discord.bot.service.CustomGameService.BLUE_TEAM;
import static kr.webgori.lolien.discord.bot.service.CustomGameService.RED_TEAM;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getEndDateOfYear;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getStartDateOfYear;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.component.AuthenticationComponent;
import kr.webgori.lolien.discord.bot.component.LeagueComponent;
import kr.webgori.lolien.discord.bot.component.RiotComponent;
import kr.webgori.lolien.discord.bot.dto.CustomGameSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamBanDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamDto;
import kr.webgori.lolien.discord.bot.dto.DataDragonVersionDto;
import kr.webgori.lolien.discord.bot.dto.league.LeagueDto;
import kr.webgori.lolien.discord.bot.dto.league.ScheduleDto;
import kr.webgori.lolien.discord.bot.dto.league.SummonerForParticipationDto;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeague;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueMatch;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueParticipant;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueParticipantStats;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueSchedule;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeam;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeamBans;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeamStats;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueScheduleRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueTeamRepository;
import kr.webgori.lolien.discord.bot.request.LeagueAddRequest;
import kr.webgori.lolien.discord.bot.request.LeagueAddResultRequest;
import kr.webgori.lolien.discord.bot.response.league.LeagueResponse;
import kr.webgori.lolien.discord.bot.response.league.ResultDto;
import kr.webgori.lolien.discord.bot.response.league.ResultResponse;
import kr.webgori.lolien.discord.bot.response.league.ScheduleResponse;
import kr.webgori.lolien.discord.bot.response.league.SummonerForParticipationResponse;
import kr.webgori.lolien.discord.bot.response.league.TeamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeagueService {
  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final LolienLeagueRepository lolienLeagueRepository;
  private final LeagueComponent leagueComponent;
  private final LolienLeagueMatchRepository lolienLeagueMatchRepository;
  private final CustomGameService customGameService;
  private final RiotComponent riotComponent;
  private final LolienLeagueTeamRepository lolienLeagueTeamRepository;
  private final LolienLeagueScheduleRepository lolienLeagueScheduleRepository;
  private final AuthenticationComponent authenticationComponent;
  private final HttpServletRequest httpServletRequest;

  /**
   * getLeagues.
   * @return Leagues
   */
  @Transactional(readOnly = true)
  public LeagueResponse getLeagues() {
    List<LolienLeague> lolienLeagues = lolienLeagueRepository.findAll();
    List<LeagueDto> leagues = Lists.newArrayList();

    for (LolienLeague lolienLeague : lolienLeagues) {
      int idx = lolienLeague.getIdx();
      String title = lolienLeague.getTitle();
      LocalDateTime createdDate = lolienLeague.getCreatedDate();

      LeagueDto leagueResponse = LeagueDto
          .builder()
          .idx(idx)
          .title(title)
          .createdDate(createdDate)
          .build();

      leagues.add(leagueResponse);
    }

    return LeagueResponse
        .builder()
        .leagues(leagues)
        .build();
  }

  /**
   * addLeague.
   * @param leagueAddRequest leagueAddRequest
   */
  @Transactional
  public void addLeague(LeagueAddRequest leagueAddRequest) {
    String title = leagueAddRequest.getTitle();
    LolienLeague lolienLeague = LolienLeague.builder().title(title).build();
    lolienLeagueRepository.save(lolienLeague);
  }

  @Transactional
  public void deleteLeague(int leagueIdx) {
    lolienLeagueRepository.deleteById(leagueIdx);
  }

  /**
   * addLeagueResult.
   * @param leagueAddResultRequest leagueAddResultRequest
   */
  @Transactional
  public void addLeagueResult(LeagueAddResultRequest leagueAddResultRequest) {
    long matchId = leagueAddResultRequest.getMatchId();

    boolean existsByGameId = lolienLeagueMatchRepository.existsByGameId(matchId);

    if (existsByGameId) {
      throw new IllegalArgumentException("이미 등록된 리그 결과 입니다.");
    }

    String entriesString = leagueAddResultRequest.getEntries();

    String[] entries = entriesString.split(",");

    if (entries.length != 10) {
      throw new IllegalArgumentException("게임 참여 인원이 잘못 되었습니다.");
    }

    int leagueIdx = leagueAddResultRequest.getLeagueIdx();
    int scheduleIdx = leagueAddResultRequest.getScheduleIdx();

    leagueComponent.addResult(leagueIdx, scheduleIdx, matchId, entries);
  }

  /**
   * getSummonersForParticipation.
   * @return SummonerForParticipationResponse
   */
  @Transactional(readOnly = true)
  public SummonerForParticipationResponse getSummonersForParticipation() {
    LocalDate startDateOfYear = getStartDateOfYear();
    LocalDate endDateOfYear = getEndDateOfYear();

    List<LolienMatch> lolienMatches = customGameService
        .getLolienMatches(startDateOfYear, endDateOfYear);

    List<SummonerForParticipationDto> summonersForParticipationDto =
        getSummonersForParticipationDto(lolienMatches);

    return SummonerForParticipationResponse
        .builder()
        .summoners(summonersForParticipationDto)
        .build();
  }

  private List<SummonerForParticipationDto> getSummonersForParticipationDto(
      List<LolienMatch> lolienMatches) {

    List<SummonerForParticipationDto> summonersForParticipationDto = Lists
        .newArrayList();

    for (LolienMatch lolienMatch : lolienMatches) {
      Set<LolienParticipant> participants = lolienMatch.getParticipants();

      for (LolienParticipant participant : participants) {
        String summonerName = participant.getLolienSummoner().getSummonerName();

        SummonerForParticipationDto summonerForParticipationDto = summonersForParticipationDto
            .stream()
            .filter(mb -> mb.getSummonerName().equals(summonerName))
            .findFirst()
            .orElse(null);

        if (Objects.isNull(summonerForParticipationDto)) {
          summonerForParticipationDto = SummonerForParticipationDto
              .builder()
              .summonerName(summonerName)
              .numberOfParticipation(1)
              .build();

          summonersForParticipationDto.add(summonerForParticipationDto);
        } else {
          summonerForParticipationDto.increaseNumberOfParticipation();
        }
      }
    }

    return summonersForParticipationDto
        .stream()
        .sorted(Comparator.comparing(SummonerForParticipationDto::getNumberOfParticipation)
            .reversed())
        .collect(Collectors.toList());
  }

  /**
   * getLeagueResultsByLeague.
   * @param leagueIndex leagueIndex
   * @param page page
   * @param size size
   * @return ResultResponse
   */
  @Transactional(readOnly = true)
  public ResultResponse getLeagueResultsByLeague(
      int leagueIndex, int scheduleIdx, int page, int size) {

    LolienLeague lolienLeague = lolienLeagueRepository
        .findById(leagueIndex)
        .orElseThrow(() -> new IllegalArgumentException("invalid league index"));

    int skip = page * size;

    List<LolienLeagueMatch> lolienLeagueMatchePages = lolienLeague
        .getLolienLeagueMatches()
        .stream()
        .filter(m -> m.getSchedule().getIdx().equals(scheduleIdx))
        .sorted(Comparator.comparing(LolienLeagueMatch::getGameCreation).reversed())
        .skip(skip)
        .limit(size)
        .collect(Collectors.toList());

    int totalPages = (int) Math.ceil(lolienLeagueMatchePages.size() / (float) size);

    return getResultResponse(lolienLeagueMatchePages, totalPages);
  }

  private ResultResponse getResultResponse(List<LolienLeagueMatch> lolienLeagueMatches,
                                           int totalPages) {

    List<ResultDto> resultsDto = Lists.newArrayList();
    List<CustomGameTeamDto> teamDtoList = Lists.newArrayList();
    List<CustomGameTeamBanDto> teamBanDtoList = Lists.newArrayList();

    List<DataDragonVersionDto> dataDragonVersions = riotComponent.getDataDragonVersions();
    Map<String, JsonObject> summonerJsonObjectMap = Maps.newHashMap();
    Map<String, JsonObject> championsJsonObjectMap = Maps.newHashMap();
    Map<String, JsonObject> itemsJsonObjectMap = Maps.newHashMap();
    Map<String, JsonArray> runesJsonArrayMap = Maps.newHashMap();

    for (LolienLeagueMatch lolienLeagueMatch : lolienLeagueMatches) {
      String gameVersion = lolienLeagueMatch.getGameVersion();

      List<LolienLeagueParticipant> participants = lolienLeagueMatch
          .getParticipants()
          .stream()
          .sorted(Comparator.comparing(LolienLeagueParticipant::getIdx))
          .collect(Collectors.toList());

      List<CustomGameSummonerDto> blueTeamSummoners = Lists.newArrayList();
      List<CustomGameSummonerDto> redTeamSummoners = Lists.newArrayList();

      for (LolienLeagueParticipant lolienParticipant : participants) {
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

        LolienLeagueParticipantStats lolienParticipantStats = lolienParticipant.getStats();

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

      List<LolienLeagueTeamStats> teams = lolienLeagueMatch
          .getTeams()
          .stream()
          .sorted(Comparator.comparing(LolienLeagueTeamStats::getIdx))
          .collect(Collectors.toList());

      User user = null;

      try {
        user = authenticationComponent.getUser(httpServletRequest);
      } catch (ExpiredJwtException | BadCredentialsException | MalformedJwtException e) {
        logger.error("", e);
      }

      for (LolienLeagueTeamStats team : teams) {
        List<LolienLeagueTeamBans> bans = team.getBans();

        for (LolienLeagueTeamBans ban : bans) {
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

      User matchUser = lolienLeagueMatch.getUser();
      boolean deleteAble = false;

      if (Objects.nonNull(user) && matchUser.equals(user)) {
        deleteAble = true;
      }

      int idx = lolienLeagueMatch.getIdx();
      long gameCreation = lolienLeagueMatch.getGameCreation();
      long gameDuration = lolienLeagueMatch.getGameDuration();
      long gameId = lolienLeagueMatch.getGameId();
      String gameMode = lolienLeagueMatch.getGameMode();
      String gameType = lolienLeagueMatch.getGameType();
      int mapId = lolienLeagueMatch.getMapId();
      String platformId = lolienLeagueMatch.getPlatformId();
      int queueId = lolienLeagueMatch.getQueueId();
      int seasonId = lolienLeagueMatch.getSeasonId();

      ResultDto resultDto = ResultDto
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

      resultsDto.add(resultDto);
    }

    return ResultResponse
        .builder()
        .results(resultsDto)
        .totalPages(totalPages)
        .build();
  }

  /**
   * addLeagueResultByFiles.
   * @param files files
   */
  public void addLeagueResultByFiles(int leagueIndex, int scheduleIdx, List<MultipartFile> files) {
    Pattern pattern = Pattern.compile("\\\\\"NAME\\\\\":\\\\\"([A-Za-z0-9가-힣 ]*)\\\\\"");

    for (MultipartFile file : files) {
      LeagueAddResultRequest leagueAddResultRequest = new LeagueAddResultRequest();
      leagueAddResultRequest.setLeagueIdx(leagueIndex);
      leagueAddResultRequest.setScheduleIdx(scheduleIdx);

      long gameId = getGameId(file);
      leagueAddResultRequest.setMatchId(gameId);

      String entries = getEntries(file, pattern);
      leagueAddResultRequest.setEntries(entries);

      addLeagueResult(leagueAddResultRequest);
    }
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

  private long getGameId(MultipartFile multipartFile) {
    String originalFilename = multipartFile.getOriginalFilename();
    originalFilename = FilenameUtils.removeExtension(originalFilename);

    if (Objects.isNull(originalFilename)) {
      throw new IllegalArgumentException("invalid league result file");
    }

    return Long.parseLong(originalFilename.replace("KR-", ""));
  }

  /**
   * deleteLeagueResult.
   * @param gameId gameId
   */
  @Transactional
  public void deleteLeagueResult(long gameId) {
    boolean existsByGameId = lolienLeagueMatchRepository.existsByGameId(gameId);

    if (!existsByGameId) {
      throw new IllegalArgumentException("리그 결과가 존재하지 않습니다.");
    }

    lolienLeagueMatchRepository.deleteByGameId(gameId);
  }

  /**
   * 팀 정보 조회.
   * @return 팀 정보
   */
  public TeamResponse getTeams() {
    List<LolienLeagueTeam> teams = lolienLeagueTeamRepository.findAll();

    return TeamResponse
        .builder()
        .teams(teams)
        .build();
  }

  /**
   * 대진표 조회.
   * @return 대진표
   */
  public ScheduleResponse getSchedules() {
    List<LolienLeagueSchedule> schedules = lolienLeagueScheduleRepository.findAll();
    List<ScheduleDto> schedulesDto = Lists.newArrayList();

    for (LolienLeagueSchedule schedule : schedules) {
      int idx = schedule.getIdx();
      LolienLeagueTeam team = schedule.getTeam();
      LolienLeagueTeam enemyTeam = schedule.getEnemyTeam();
      LocalDateTime matchDateTime = schedule.getMatchDateTime();

      ScheduleDto scheduleDto = ScheduleDto
          .builder()
          .idx(idx)
          .team(team)
          .enemyTeam(enemyTeam)
          .matchDateTime(matchDateTime)
          .build();

      schedulesDto.add(scheduleDto);
    }

    return ScheduleResponse
        .builder()
        .schedules(schedulesDto)
        .build();
  }
}
