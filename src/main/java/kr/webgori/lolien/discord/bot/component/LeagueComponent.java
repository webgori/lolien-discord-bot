package kr.webgori.lolien.discord.bot.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.entity.league.*;
import kr.webgori.lolien.discord.bot.exception.LeagueNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LoLienLeagueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.match.dto.*;
import net.rithms.riot.constant.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Component
public class LeagueComponent {
  private final LoLienLeagueRepository loLienLeagueRepository;
  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LoLienLeagueMatchRepository loLienLeagueMatchRepository;
  private final Gson gson;

  @Value("${riot.api.key}")
  private String riotApiKey;

  /**
   * addResult.
   * @param leagueIdx leagueIdx
   * @param matchId matchId
   * @param entries entries
   */
  public void addResult(int leagueIdx, long matchId, String[] entries) {
    LoLienLeague loLienLeague = loLienLeagueRepository
            .findById(leagueIdx)
            .orElseThrow(() -> new LeagueNotFoundException("invalid league idx"));

    for (String summonerName : entries) {
      boolean hasSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

      if (!hasSummonerName) {
        throw new IllegalArgumentException("register summoner first");
      }
    }

    try {
      ApiConfig config = new ApiConfig().setKey(riotApiKey);
      RiotApi riotApi = new RiotApi(config);
      Match match = riotApi.getMatch(Platform.KR, matchId);

      long gameCreation = match.getGameCreation();
      long gameDuration = match.getGameDuration();
      long gameId = match.getGameId();
      String gameMode = match.getGameMode();
      String gameType = match.getGameType();
      String gameVersion = match.getGameVersion();
      int mapId = match.getMapId();

      String platformId = match.getPlatformId();
      int queueId = match.getQueueId();
      int seasonId = match.getSeasonId();

      Set<LoLienLeagueParticipant> loLienLeagueParticipantSet = Sets.newHashSet();
      Set<LoLienLeagueTeamStats> loLienLeagueTeamStatsSet = Sets.newHashSet();

      LoLienLeagueMatch loLienLeagueMatch = LoLienLeagueMatch
              .builder()
              .lolienLeague(loLienLeague)
              .gameCreation(gameCreation)
              .gameDuration(gameDuration)
              .gameId(gameId)
              .gameMode(gameMode)
              .gameType(gameType)
              .gameVersion(gameVersion)
              .mapId(mapId)
              .participants(loLienLeagueParticipantSet)
              .platformId(platformId)
              .queueId(queueId)
              .seasonId(seasonId)
              .teams(loLienLeagueTeamStatsSet)
              .build();

      List<Participant> participants = match.getParticipants();

      for (int i = 0; i < participants.size(); i++) {
        Participant participant = participants.get(i);

        int championId = participant.getChampionId();
        int participantId = participant.getParticipantId();
        int spell1Id = participant.getSpell1Id();
        int spell2Id = participant.getSpell2Id();
        int teamId = participant.getTeamId();

        ParticipantStats stats = participant.getStats();
        int altarsCaptured = stats.getAltarsCaptured();
        int altarsNeutralized = stats.getAltarsNeutralized();
        int assists = stats.getAssists();
        int champLevel = stats.getChampLevel();
        int combatPlayerScore = stats.getCombatPlayerScore();
        long damageDealtToObjectives = stats.getDamageDealtToObjectives();
        long damageDealtToTurrets = stats.getDamageDealtToTurrets();
        long damageSelfMitigated = stats.getDamageSelfMitigated();
        int deaths = stats.getDeaths();
        int doubleKills = stats.getDoubleKills();
        boolean firstBloodAssist = stats.isFirstBloodAssist();
        boolean firstBloodKill = stats.isFirstBloodKill();
        boolean firstInhibitorAssist = stats.isFirstInhibitorAssist();
        boolean firstInhibitorKill = stats.isFirstInhibitorKill();
        boolean firstTowerAssist = stats.isFirstTowerAssist();
        boolean firstTowerKill = stats.isFirstTowerKill();
        int goldEarned = stats.getGoldEarned();
        int goldSpent = stats.getGoldSpent();
        int inhibitorKills = stats.getInhibitorKills();
        int item0 = stats.getItem0();
        int item1 = stats.getItem1();
        int item2 = stats.getItem2();
        int item3 = stats.getItem3();
        int item4 = stats.getItem4();
        int item5 = stats.getItem5();
        int item6 = stats.getItem6();
        int killingSprees = stats.getKillingSprees();
        int kills = stats.getKills();
        int largestCriticalStrike = stats.getLargestCriticalStrike();
        int largestKillingSpree = stats.getLargestKillingSpree();
        int largestMultiKill = stats.getLargestMultiKill();
        int longestTimeSpentLiving = stats.getLongestTimeSpentLiving();
        long magicDamageDealt = stats.getMagicDamageDealt();
        long magicDamageDealtToChampions = stats.getMagicDamageDealtToChampions();
        long magicalDamageTaken = stats.getMagicalDamageTaken();
        int neutralMinionsKilled = stats.getNeutralMinionsKilled();
        int neutralMinionsKilledEnemyJungle = stats.getNeutralMinionsKilledEnemyJungle();
        int neutralMinionsKilledTeamJungle = stats.getNeutralMinionsKilledTeamJungle();
        int nodeCapture = stats.getNodeCapture();
        int nodeCaptureAssist = stats.getNodeCaptureAssist();
        int nodeNeutralize = stats.getNodeNeutralize();
        int nodeNeutralizeAssist = stats.getNodeNeutralizeAssist();
        int objectivePlayerScore = stats.getObjectivePlayerScore();
        int participantId1 = stats.getParticipantId();
        int pentaKills = stats.getPentaKills();
        long physicalDamageDealt = stats.getPhysicalDamageDealt();
        long physicalDamageDealtToChampions = stats.getPhysicalDamageDealtToChampions();
        long physicalDamageTaken = stats.getPhysicalDamageTaken();
        int quadraKills = stats.getQuadraKills();
        int sightWardsBoughtInGame = stats.getSightWardsBoughtInGame();
        int teamObjective = stats.getTeamObjective();
        int timeCCingOthers = stats.getTimeCCingOthers();
        long totalDamageDealt = stats.getTotalDamageDealt();
        long totalDamageDealtToChampions = stats.getTotalDamageDealtToChampions();
        long totalDamageTaken = stats.getTotalDamageTaken();
        long totalHeal = stats.getTotalHeal();
        int totalMinionsKilled = stats.getTotalMinionsKilled();
        int totalPlayerScore = stats.getTotalPlayerScore();
        int totalScoreRank = stats.getTotalScoreRank();
        int totalTimeCrowdControlDealt = stats.getTotalTimeCrowdControlDealt();
        int totalUnitsHealed = stats.getTotalUnitsHealed();
        int tripleKills = stats.getTripleKills();
        long trueDamageDealt = stats.getTrueDamageDealt();
        long trueDamageDealtToChampions = stats.getTrueDamageDealtToChampions();
        long trueDamageTaken = stats.getTrueDamageTaken();
        int turretKills = stats.getTurretKills();
        int unrealKills = stats.getUnrealKills();
        long visionScore = stats.getVisionScore();
        int visionWardsBoughtInGame = stats.getVisionWardsBoughtInGame();
        int wardsKilled = stats.getWardsKilled();
        int wardsPlaced = stats.getWardsPlaced();
        boolean win = stats.isWin();
        int perk0 = stats.getPerk0();
        int perk1 = stats.getPerk1();
        int perk2 = stats.getPerk2();
        int perk3 = stats.getPerk3();
        int perk4 = stats.getPerk4();
        int perk5 = stats.getPerk5();
        long perk0Var1 = stats.getPerk0Var1();
        long perk0Var2 = stats.getPerk0Var2();
        long perk0Var3 = stats.getPerk0Var3();
        long perk1Var1 = stats.getPerk1Var1();
        long perk1Var2 = stats.getPerk1Var2();
        long perk1Var3 = stats.getPerk1Var3();
        long perk2Var1 = stats.getPerk2Var1();
        long perk2Var2 = stats.getPerk2Var2();
        long perk2Var3 = stats.getPerk2Var3();
        long perk3Var1 = stats.getPerk3Var1();
        long perk3Var2 = stats.getPerk3Var2();
        long perk3Var3 = stats.getPerk3Var3();
        long perk4Var1 = stats.getPerk4Var1();
        long perk4Var2 = stats.getPerk4Var2();
        long perk4Var3 = stats.getPerk4Var3();
        long perk5Var1 = stats.getPerk5Var1();
        long perk5Var2 = stats.getPerk5Var2();
        long perk5Var3 = stats.getPerk5Var3();
        long playerScore0 = stats.getPlayerScore0();
        long playerScore1 = stats.getPlayerScore1();
        long playerScore2 = stats.getPlayerScore2();
        long playerScore3 = stats.getPlayerScore3();
        long playerScore4 = stats.getPlayerScore4();
        long playerScore5 = stats.getPlayerScore5();
        long playerScore6 = stats.getPlayerScore6();
        long playerScore7 = stats.getPlayerScore7();
        long playerScore8 = stats.getPlayerScore8();
        long playerScore9 = stats.getPlayerScore9();
        int perkPrimaryStyle = stats.getPerkPrimaryStyle();
        int perkSubStyle = stats.getPerkSubStyle();
        int statPerk0 = stats.getStatPerk0();
        int statPerk1 = stats.getStatPerk1();
        int statPerk2 = stats.getStatPerk2();

        LoLienLeagueParticipantStats loLienLeagueParticipantStats = LoLienLeagueParticipantStats
                .builder()
                .altarsCaptured(altarsCaptured)
                .altarsNeutralized(altarsNeutralized)
                .assists(assists)
                .champLevel(champLevel)
                .combatPlayerScore(combatPlayerScore)
                .damageDealtToObjectives(damageDealtToObjectives)
                .damageDealtToTurrets(damageDealtToTurrets)
                .damageSelfMitigated(damageSelfMitigated)
                .deaths(deaths)
                .doubleKills(doubleKills)
                .firstBloodAssist(firstBloodAssist)
                .firstBloodKill(firstBloodKill)
                .firstInhibitorAssist(firstInhibitorAssist)
                .firstInhibitorKill(firstInhibitorKill)
                .firstTowerAssist(firstTowerAssist)
                .firstTowerKill(firstTowerKill)
                .goldEarned(goldEarned)
                .goldSpent(goldSpent)
                .inhibitorKills(inhibitorKills)
                .item0(item0)
                .item1(item1)
                .item2(item2)
                .item3(item3)
                .item4(item4)
                .item5(item5)
                .item6(item6)
                .killingSprees(killingSprees)
                .kills(kills)
                .largestCriticalStrike(largestCriticalStrike)
                .largestKillingSpree(largestKillingSpree)
                .largestMultiKill(largestMultiKill)
                .longestTimeSpentLiving(longestTimeSpentLiving)
                .magicDamageDealt(magicDamageDealt)
                .magicDamageDealtToChampions(magicDamageDealtToChampions)
                .magicalDamageTaken(magicalDamageTaken)
                .neutralMinionsKilled(neutralMinionsKilled)
                .neutralMinionsKilledEnemyJungle(neutralMinionsKilledEnemyJungle)
                .neutralMinionsKilledTeamJungle(neutralMinionsKilledTeamJungle)
                .nodeCapture(nodeCapture)
                .nodeCaptureAssist(nodeCaptureAssist)
                .nodeNeutralize(nodeNeutralize)
                .nodeNeutralizeAssist(nodeNeutralizeAssist)
                .objectivePlayerScore(objectivePlayerScore)
                .participantId(participantId1)
                .pentaKills(pentaKills)
                .physicalDamageDealt(physicalDamageDealt)
                .physicalDamageDealtToChampions(physicalDamageDealtToChampions)
                .physicalDamageTaken(physicalDamageTaken)
                .quadraKills(quadraKills)
                .sightWardsBoughtInGame(sightWardsBoughtInGame)
                .teamObjective(teamObjective)
                .timeCCingOthers(timeCCingOthers)
                .totalDamageDealt(totalDamageDealt)
                .totalDamageDealtToChampions(totalDamageDealtToChampions)
                .totalDamageTaken(totalDamageTaken)
                .totalHeal(totalHeal)
                .totalMinionsKilled(totalMinionsKilled)
                .totalPlayerScore(totalPlayerScore)
                .totalScoreRank(totalScoreRank)
                .totalTimeCrowdControlDealt(totalTimeCrowdControlDealt)
                .totalUnitsHealed(totalUnitsHealed)
                .tripleKills(tripleKills)
                .trueDamageDealt(trueDamageDealt)
                .trueDamageDealtToChampions(trueDamageDealtToChampions)
                .trueDamageTaken(trueDamageTaken)
                .turretKills(turretKills)
                .unrealKills(unrealKills)
                .visionScore(visionScore)
                .visionWardsBoughtInGame(visionWardsBoughtInGame)
                .wardsKilled(wardsKilled)
                .wardsPlaced(wardsPlaced)
                .win(win)
                .perk0(perk0)
                .perk1(perk1)
                .perk2(perk2)
                .perk3(perk3)
                .perk4(perk4)
                .perk5(perk5)
                .perk0Var1(perk0Var1)
                .perk0Var2(perk0Var2)
                .perk0Var3(perk0Var3)
                .perk1Var1(perk1Var1)
                .perk1Var2(perk1Var2)
                .perk1Var3(perk1Var3)
                .perk2Var1(perk2Var1)
                .perk2Var2(perk2Var2)
                .perk2Var3(perk2Var3)
                .perk3Var1(perk3Var1)
                .perk3Var2(perk3Var2)
                .perk3Var3(perk3Var3)
                .perk4Var1(perk4Var1)
                .perk4Var2(perk4Var2)
                .perk4Var3(perk4Var3)
                .perk5Var1(perk5Var1)
                .perk5Var2(perk5Var2)
                .perk5Var3(perk5Var3)
                .playerScore0(playerScore0)
                .playerScore1(playerScore1)
                .playerScore2(playerScore2)
                .playerScore3(playerScore3)
                .playerScore4(playerScore4)
                .playerScore5(playerScore5)
                .playerScore6(playerScore6)
                .playerScore7(playerScore7)
                .playerScore8(playerScore8)
                .playerScore9(playerScore9)
                .perkPrimaryStyle(perkPrimaryStyle)
                .perkSubStyle(perkSubStyle)
                .statPerk0(statPerk0)
                .statPerk1(statPerk1)
                .statPerk2(statPerk2)
                .build();

        String summonerName = entries[i];
        LoLienSummoner bySummonerName = loLienSummonerRepository
                .findBySummonerName(summonerName);

        LoLienLeagueParticipant loLienLeagueParticipant = LoLienLeagueParticipant
                .builder()
                .match(loLienLeagueMatch)
                .championId(championId)
                .participantId(participantId)
                .spell1Id(spell1Id)
                .spell2Id(spell2Id)
                .stats(loLienLeagueParticipantStats)
                .teamId(teamId)
                .loLienSummoner(bySummonerName)
                .build();

        loLienLeagueParticipantStats.setParticipant(loLienLeagueParticipant);

        loLienLeagueParticipantSet.add(loLienLeagueParticipant);
      }

      List<TeamStats> teams = match.getTeams();
      List<LoLienLeagueTeamBans> loLienLeagueTeamBansList = Lists.newArrayList();

      for (TeamStats teamStats : teams) {
        int baronKills = teamStats.getBaronKills();
        int dominionVictoryScore = teamStats.getDominionVictoryScore();
        int dragonKills = teamStats.getDragonKills();
        boolean firstBaron = teamStats.isFirstBaron();
        boolean firstBlood = teamStats.isFirstBlood();
        boolean firstDragon = teamStats.isFirstDragon();
        boolean firstInhibitor = teamStats.isFirstInhibitor();
        boolean firstRiftHerald = teamStats.isFirstRiftHerald();
        boolean firstTower = teamStats.isFirstTower();
        int inhibitorKills = teamStats.getInhibitorKills();
        int riftHeraldKills = teamStats.getRiftHeraldKills();
        int teamId = teamStats.getTeamId();
        int towerKills = teamStats.getTowerKills();
        int vilemawKills = teamStats.getVilemawKills();
        String win = teamStats.getWin();

        LoLienLeagueTeamStats loLienLeagueTeamStats = LoLienLeagueTeamStats
                .builder()
                .match(loLienLeagueMatch)
                .bans(loLienLeagueTeamBansList)
                .baronKills(baronKills)
                .dominionVictoryScore(dominionVictoryScore)
                .dragonKills(dragonKills)
                .firstBaron(firstBaron)
                .firstBlood(firstBlood)
                .firstDragon(firstDragon)
                .firstInhibitor(firstInhibitor)
                .firstRiftHerald(firstRiftHerald)
                .firstTower(firstTower)
                .inhibitorKills(inhibitorKills)
                .riftHeraldKills(riftHeraldKills)
                .teamId(teamId)
                .towerKills(towerKills)
                .vilemawKills(vilemawKills)
                .win(win)
                .build();

        List<TeamBans> bans = teamStats.getBans();

        for (TeamBans teamBans : bans) {
          int championId = teamBans.getChampionId();
          int pickTurn = teamBans.getPickTurn();

          LoLienLeagueTeamBans loLienLeagueTeamBans = LoLienLeagueTeamBans
                  .builder()
                  .teamStats(loLienLeagueTeamStats)
                  .championId(championId)
                  .pickTurn(pickTurn)
                  .build();

          loLienLeagueTeamBansList.add(loLienLeagueTeamBans);
        }

        loLienLeagueTeamStatsSet.add(loLienLeagueTeamStats);
      }

      loLienLeagueMatchRepository.save(loLienLeagueMatch);

      /*for (String summonerName : entries) {
        HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
        boolean hasHashKey = opsForHash.hasKey(REDIS_MOST_CHAMPS_KEY, summonerName);
        if (hasHashKey) {
          opsForHash.delete(REDIS_MOST_CHAMPS_KEY, summonerName);
        }
        getMostChamp(summonerName, 3);
      }*/
    } catch (RiotApiException e) {
      int errorCode = e.getErrorCode();
      if (errorCode == RiotApiException.FORBIDDEN) {
        throw new IllegalArgumentException("api-key-expired");
      } else {
        logger.error("{}", e.getMessage());
        throw new IllegalArgumentException("riotApiException");
      }
    }
  }
}