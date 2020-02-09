package kr.webgori.lolien.discord.bot.service.impl;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import kr.webgori.lolien.discord.bot.component.CustomGameComponent;
import kr.webgori.lolien.discord.bot.entity.LoLienMatch;
import kr.webgori.lolien.discord.bot.entity.LoLienParticipant;
import kr.webgori.lolien.discord.bot.entity.LoLienParticipantStats;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.repository.LoLienMatchRepository;
import kr.webgori.lolien.discord.bot.request.CustomGameAddResultRequest;
import kr.webgori.lolien.discord.bot.response.CustomGameResponse;
import kr.webgori.lolien.discord.bot.response.CustomGameSummonerResponse;
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
  private final LoLienMatchRepository loLienMatchRepository;
  private final CustomGameComponent customGameComponent;

  @Transactional
  @Override
  public void addResult(CustomGameAddResultRequest customGameAddResultRequest) {
    long matchId = customGameAddResultRequest.getMatchId();
    String entriesString = customGameAddResultRequest.getEntries();

    boolean existsByGameId = loLienMatchRepository.existsByGameId(matchId);

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
    List<LoLienMatch> loLienMatches = loLienMatchRepository.findTop5AllByOrderByGameCreationDesc();

    List<CustomGameResponse> customGames = Lists.newArrayList();

    for (LoLienMatch loLienMatch : loLienMatches) {
      int idx = loLienMatch.getIdx();
      long gameCreation = loLienMatch.getGameCreation();
      long gameDuration = loLienMatch.getGameDuration();
      long gameId = loLienMatch.getGameId();
      String gameMode = loLienMatch.getGameMode();
      String gameType = loLienMatch.getGameType();
      String gameVersion = loLienMatch.getGameVersion();
      int mapId = loLienMatch.getMapId();
      String platformId = loLienMatch.getPlatformId();
      int queueId = loLienMatch.getQueueId();
      int seasonId = loLienMatch.getSeasonId();

      Set<LoLienParticipant> participants = loLienMatch.getParticipants();

      List<CustomGameSummonerResponse> customGameSummonerResponses = Lists.newArrayList();

      for (LoLienParticipant loLienParticipant : participants) {
        LoLienSummoner loLienSummoner = loLienParticipant.getLoLienSummoner();
        int spell1Id = loLienParticipant.getSpell1Id();
        int spell2Id = loLienParticipant.getSpell2Id();

        int loLienSummonerIdx = loLienSummoner.getIdx();
        String summonerName = loLienSummoner.getSummonerName();

        int championId = loLienParticipant.getChampionId();

        LoLienParticipantStats loLienParticipantStats = loLienParticipant.getStats();

        long totalDamageDealtToChampions = loLienParticipantStats.getTotalDamageDealtToChampions();
        int wardsPlaced = loLienParticipantStats.getWardsPlaced();

        int kills = loLienParticipantStats.getKills();
        int deaths = loLienParticipantStats.getDeaths();
        int assists = loLienParticipantStats.getAssists();

        int champLevel = loLienParticipantStats.getChampLevel();
        int totalMinionsKilled = loLienParticipantStats.getTotalMinionsKilled();

        int item0 = loLienParticipantStats.getItem0();
        int item1 = loLienParticipantStats.getItem1();
        int item2 = loLienParticipantStats.getItem2();
        int item3 = loLienParticipantStats.getItem3();
        int item4 = loLienParticipantStats.getItem4();
        int item5 = loLienParticipantStats.getItem5();
        int item6 = loLienParticipantStats.getItem6();

        CustomGameSummonerResponse customGameSummonerResponse = CustomGameSummonerResponse
            .builder()
            .idx(loLienSummonerIdx)
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
            .build();

        customGameSummonerResponses.add(customGameSummonerResponse);
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
          .summoners(customGameSummonerResponses)
          .build();

      customGames.add(customGameResponse);
    }

    return CustomGamesResponse
        .builder()
        .customGames(customGames)
        .build();
  }
}