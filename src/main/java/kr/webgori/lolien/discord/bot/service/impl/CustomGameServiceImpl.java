package kr.webgori.lolien.discord.bot.service.impl;

import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.dto.CustomGameSummonerDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamBanDto;
import kr.webgori.lolien.discord.bot.dto.CustomGameTeamDto;
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
  public CustomGamesResponse getCustomGames() {
    List<LolienMatch> lolienMatches = lolienMatchRepository.findTop5AllByOrderByGameCreationDesc();

    return getCustomGamesResponse(lolienMatches);
  }

  @Override
  public CustomGamesResponse getCustomGamesBySummoner(String targetSummonerName) {
    LolienSummoner lolienSummoner = lolienSummonerRepository.findBySummonerName(targetSummonerName);

    if (Objects.isNull(lolienSummoner)) {
      throw new SummonerNotFoundException("");
    }

    List<LolienMatch> lolienMatches = lolienSummoner.getParticipants()
        .stream()
        .map(LolienParticipant::getMatch)
        .sorted(Comparator.comparing(LolienMatch::getIdx))
        .collect(Collectors.toList());

    return getCustomGamesResponse(lolienMatches);
  }

  private CustomGamesResponse getCustomGamesResponse(List<LolienMatch> lolienMatches) {
    List<CustomGameResponse> customGames = Lists.newArrayList();
    List<CustomGameTeamDto> teamDtoList = Lists.newArrayList();
    List<CustomGameTeamBanDto> teamBanDtoList = Lists.newArrayList();

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
        LolienSummoner lolienSummoner = lolienParticipant.getLolienSummoner();
        int spell1Id = lolienParticipant.getSpell1Id();
        int spell2Id = lolienParticipant.getSpell2Id();

        int lolienSummonerIdx = lolienSummoner.getIdx();
        String summonerName = lolienSummoner.getSummonerName();

        int championId = lolienParticipant.getChampionId();

        LolienParticipantStats lolienParticipantStats = lolienParticipant.getStats();

        long totalDamageDealtToChampions = lolienParticipantStats.getTotalDamageDealtToChampions();
        int wardsPlaced = lolienParticipantStats.getWardsPlaced();

        int kills = lolienParticipantStats.getKills();
        int deaths = lolienParticipantStats.getDeaths();
        int assists = lolienParticipantStats.getAssists();

        int champLevel = lolienParticipantStats.getChampLevel();
        int totalMinionsKilled = lolienParticipantStats.getTotalMinionsKilled();

        int item0 = lolienParticipantStats.getItem0();
        int item1 = lolienParticipantStats.getItem1();
        int item2 = lolienParticipantStats.getItem2();
        int item3 = lolienParticipantStats.getItem3();
        int item4 = lolienParticipantStats.getItem4();
        int item5 = lolienParticipantStats.getItem5();
        int item6 = lolienParticipantStats.getItem6();

        int teamId = lolienParticipant.getTeamId();
        boolean win = lolienParticipantStats.getWin();

        CustomGameSummonerDto customGameSummonerDto = CustomGameSummonerDto
            .builder()
            .idx(lolienSummonerIdx)
            .summonerName(summonerName)
            .championId(championId)
            .totalDamageDealtToChampions(totalDamageDealtToChampions)
            .spell1Id(spell1Id)
            .spell2Id(spell2Id)
            .kills(kills)
            .deaths(deaths)
            .assists(assists)
            .champLevel(champLevel)
            .totalMinionsKilled(totalMinionsKilled)
            .item0(item0)
            .item1(item1)
            .item2(item2)
            .item3(item3)
            .item4(item4)
            .item5(item5)
            .item6(item6)
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
        .build();
  }
}
