package kr.webgori.lolien.discord.bot.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.component.RiotComponent;
import kr.webgori.lolien.discord.bot.dto.CustomGameSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamBanDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamDto;
import kr.webgori.lolien.discord.bot.dto.DataDragonVersionDto;
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
        String championName = riotComponent.getChampionName(championsJsonObject,
            closeDataDragonVersion, championId);

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
        String item0Name = riotComponent.getItemName(itemsJsonObject, closeDataDragonVersion,
            item0);
        String item0Description = riotComponent
            .getItemDescription(itemsJsonObject, closeDataDragonVersion, item0);

        int item1 = lolienParticipantStats.getItem1();
        String item1Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item1);
        String item1Name = riotComponent.getItemName(itemsJsonObject, closeDataDragonVersion,
            item1);
        String item1Description = riotComponent
            .getItemDescription(itemsJsonObject, closeDataDragonVersion, item1);

        int item2 = lolienParticipantStats.getItem2();
        String item2Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item2);
        String item2Name = riotComponent.getItemName(itemsJsonObject, closeDataDragonVersion,
            item2);
        String item2Description = riotComponent
            .getItemDescription(itemsJsonObject, closeDataDragonVersion, item2);

        int item3 = lolienParticipantStats.getItem3();
        String item3Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item3);
        String item3Name = riotComponent.getItemName(itemsJsonObject, closeDataDragonVersion,
            item3);
        String item3Description = riotComponent
            .getItemDescription(itemsJsonObject, closeDataDragonVersion, item3);

        int item4 = lolienParticipantStats.getItem4();
        String item4Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item4);
        String item4Name = riotComponent.getItemName(itemsJsonObject, closeDataDragonVersion,
            item4);
        String item4Description = riotComponent
            .getItemDescription(itemsJsonObject, closeDataDragonVersion, item4);

        int item5 = lolienParticipantStats.getItem5();
        String item5Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item5);
        String item5Name = riotComponent.getItemName(itemsJsonObject, closeDataDragonVersion,
            item5);
        String item5Description = riotComponent
            .getItemDescription(itemsJsonObject, closeDataDragonVersion, item5);

        int item6 = lolienParticipantStats.getItem6();
        String item6Url = riotComponent.getItemUrl(itemsJsonObject, closeDataDragonVersion, item6);
        String item6Name = riotComponent.getItemName(itemsJsonObject, closeDataDragonVersion,
            item6);
        String item6Description = riotComponent
            .getItemDescription(itemsJsonObject, closeDataDragonVersion, item6);

        JsonArray runesJsonArray;

        if (runesJsonArrayMap.containsKey(closeDataDragonVersion)) {
          runesJsonArray = runesJsonArrayMap.get(closeDataDragonVersion);
        } else {
          runesJsonArray = riotComponent.getRuneJsonArray(closeDataDragonVersion);
          runesJsonArrayMap.put(closeDataDragonVersion, runesJsonArray);
        }

        int perkPrimaryStyle = lolienParticipantStats.getPerkPrimaryStyle();
        String primaryRuneUrl = riotComponent.getRuneUrl(runesJsonArray, closeDataDragonVersion,
            perkPrimaryStyle);
        String primaryRuneName = riotComponent.getRuneName(runesJsonArray, closeDataDragonVersion,
            perkPrimaryStyle);
        String primaryRuneDescription = riotComponent
            .getRuneDescription(runesJsonArray, closeDataDragonVersion, perkPrimaryStyle);

        int perkSubStyle = lolienParticipantStats.getPerkSubStyle();
        String subRuneUrl = riotComponent.getRuneUrl(runesJsonArray, closeDataDragonVersion,
            perkSubStyle);
        String subRuneName = riotComponent.getRuneName(runesJsonArray, closeDataDragonVersion,
            perkSubStyle);
        String subRuneDescription = riotComponent
            .getRuneDescription(runesJsonArray, closeDataDragonVersion, perkSubStyle);

        int teamId = lolienParticipant.getTeamId();
        boolean win = lolienParticipantStats.getWin();

        LolienSummoner lolienSummoner = lolienParticipant.getLolienSummoner();
        int lolienSummonerIdx = lolienSummoner.getIdx();
        String summonerName = lolienSummoner.getSummonerName();

        int spell1Id = lolienParticipant.getSpell1Id();
        String spell1Url = riotComponent.getSpellUrl(summonerJsonObject, closeDataDragonVersion,
            spell1Id);
        String spell1Name = riotComponent.getSpellName(summonerJsonObject, closeDataDragonVersion,
            spell1Id);
        String spell1Description = riotComponent.getSpellDescription(summonerJsonObject,
            closeDataDragonVersion, spell1Id);

        int spell2Id = lolienParticipant.getSpell2Id();
        String spell2Url = riotComponent.getSpellUrl(summonerJsonObject, closeDataDragonVersion,
            spell2Id);
        String spell2Name = riotComponent.getSpellName(summonerJsonObject, closeDataDragonVersion,
            spell2Id);
        String spell2Description = riotComponent.getSpellDescription(summonerJsonObject,
            closeDataDragonVersion, spell2Id);

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
}
