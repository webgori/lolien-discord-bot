package kr.webgori.lolien.discord.bot.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import kr.webgori.lolien.discord.bot.entity.*;
import kr.webgori.lolien.discord.bot.repository.ChampRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienMatchRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienParticipantRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import kr.webgori.lolien.discord.bot.spring.LinkedIntegerLongHashMapTypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.match.dto.*;
import net.rithms.riot.constant.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.*;

@Slf4j
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS")
@RequiredArgsConstructor
@Component
public class CustomGameComponent {
  private static final String REDIS_MOST_CHAMPS_KEY = "lolien-discord-bot:most-champs";

  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LoLienMatchRepository loLienMatchRepository;
  private final LoLienParticipantRepository loLienParticipantRepository;
  private final ChampRepository champRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final Gson gson;

  @Value("${riot.api.key}")
  private String riotApiKey;

  /**
   * execute.
   * @param event event
   */
  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();
    String receivedMessage = event.getMessage().getContentDisplay();
    List<String> commands = Lists.newArrayList(receivedMessage.split(" "));

    if (commands.size() < 2) {
      sendSyntax(textChannel);
      return;
    }

    String arg1 = commands.get(1);

    switch (arg1) {
      case "결과":
        if (commands.size() < 3) {
          sendSyntax(textChannel);
          return;
        }

        String arg2 = commands.get(2);

        switch (arg2) {
          case "조회":
            if (commands.size() != 3) {
              sendGetResultSyntax(textChannel);
              return;
            }

            List<LoLienMatch> matches = loLienMatchRepository
                .findTop5AllByOrderByGameCreationDesc();

            List<String> latestCustomGames = Lists.newArrayList();

            for (LoLienMatch loLienMatch : matches) {
              Long gameCreation = loLienMatch.getGameCreation();
              ZoneId zone = ZoneId.systemDefault();
              DateTimeFormatter df = DateTimeFormatter
                  .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);
              String gameCreationDateTime = df.format(Instant.ofEpochMilli(gameCreation));

              Long gameDuration = loLienMatch.getGameDuration();
              Duration duration = Duration.ofSeconds(gameDuration);
              long hour = duration.toHours();
              long minute = duration.toMinutes();

              if (hour > 0) {
                minute = minute - hour * 60;
              }

              Set<LoLienParticipant> participants = loLienMatch.getParticipants();
              LoLienParticipant loLienParticipant = Collections
                  .max(participants,
                      Comparator.comparing(s -> s.getStats()
                          .getTotalDamageDealtToChampions()));

              LoLienSummoner loLienSummoner = loLienParticipant.getLoLienSummoner();
              String summonerName = loLienSummoner.getSummonerName();

              LoLienParticipantStats stats = loLienParticipant.getStats();
              Long totalDamageDealtToChampions = stats.getTotalDamageDealtToChampions();
              DecimalFormat decimalFormat = new DecimalFormat("###,###");
              String totalDamageToChampions = decimalFormat.format(totalDamageDealtToChampions);

              String message;

              if (hour == 0) {
                message = String
                    .format("날짜: %s, 진행 시간: %s분, 최고딜량: %s (%s)",
                        gameCreationDateTime, minute, summonerName,
                        totalDamageToChampions);
              } else {
                message = String
                    .format("날짜: %s, 진행 시간: %s시간 %s분, 최고딜량: %s (%s)",
                        gameCreationDateTime, hour, minute, summonerName,
                        totalDamageToChampions);
              }

              latestCustomGames.add(message);
            }

            latestCustomGames = Lists.reverse(latestCustomGames);

            for (int i = 0; i < latestCustomGames.size(); i++) {
              String message = String.format("0%s. %s", i + 1, latestCustomGames.get(i));
              sendMessage(textChannel, message);
            }
            break;
          case "삭제": {
            String arg3 = commands.get(3);
            String matchHistoryUrlPattern
                = "http://matchhistory.leagueoflegends.co.kr/ko/#match-details/KR/[0-9]+/[0-9]+.+";
            Pattern pattern = Pattern.compile(matchHistoryUrlPattern);
            Matcher matcher = pattern.matcher(arg3);

            if (!matcher.find()) {
              sendErrorMessage(textChannel, "대전기록 URL 형식이 올바르지 않습니다.", Color.RED);
              return;
            }

            pattern = Pattern.compile("[0-9]+");
            matcher = pattern.matcher(arg3);
            long matchId = 0;

            if (matcher.find()) {
              matchId = Long.parseLong(matcher.group());
            }

            boolean existsByGameId = loLienMatchRepository.existsByGameId(matchId);

            if (!existsByGameId) {
              sendErrorMessage(textChannel, "내전 데이터가 존재하지 않습니다.", Color.RED);
              return;
            }

            loLienMatchRepository.deleteByGameId(matchId);
            break;
          }
          default:
        }
        break;
      case "참여횟수":
        if (commands.size() == 2) {
          Map<String, Integer> top5OfCustomGamePlayCountMaps = Maps.newLinkedHashMap();
          List<LoLienParticipant> participants = loLienParticipantRepository.findAll();
          for (LoLienParticipant participant : participants) {
            String summonerName = participant.getLoLienSummoner().getSummonerName();
            if (top5OfCustomGamePlayCountMaps.containsKey(summonerName)) {
              Integer customGamePlayCount = top5OfCustomGamePlayCountMaps.get(summonerName);
              top5OfCustomGamePlayCountMaps.put(summonerName, customGamePlayCount + 1);
            } else {
              top5OfCustomGamePlayCountMaps.put(summonerName, 1);
            }
          }

          top5OfCustomGamePlayCountMaps = top5OfCustomGamePlayCountMaps.entrySet().stream()
              .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
              .limit(5)
              .collect(Collectors
                  .toMap(Map.Entry::getKey, Map.Entry::getValue,
                      (oldValue, newValue) -> oldValue, LinkedHashMap::new));

          if (top5OfCustomGamePlayCountMaps.size() > 0) {
            String message = "-- 현재 내전 참여횟수 TOP5 --";
            sendMessage(textChannel, message);

            int rank = 1;
            for (Map.Entry<String, Integer> entry : top5OfCustomGamePlayCountMaps.entrySet()) {
              String summonerName = entry.getKey();
              Integer customGamePlayCount = entry.getValue();

              message = String.format("0%s. %s (%s회)", rank, summonerName, customGamePlayCount);
              sendMessage(textChannel, message);
              rank++;
            }
          }
        } else {
          StringBuilder summonerNamesBuilder = new StringBuilder();

          for (int i = 2; i < commands.size(); i++) {
            summonerNamesBuilder.append(commands.get(i));
          }

          String summonerNames = summonerNamesBuilder.toString();
          List<String> summonerNameList = Lists.newArrayList(summonerNames.split(","));

          for (String summonerName : summonerNameList) {
            boolean existsSummonerName = loLienSummonerRepository
                .existsBySummonerName(summonerName);

            if (!existsSummonerName) {
              String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
              sendErrorMessage(textChannel, errorMessage, Color.RED);
              continue;
            }

            int customGamePlayCount = loLienParticipantRepository
                .findByLoLienSummonerSummonerName(summonerName).size();
            String message = String.format("%s (%s회)", summonerName, customGamePlayCount);
            sendMessage(textChannel, message);
          }
        }
        break;
      case "모스트":
        StringBuilder summonerNameBuilder = new StringBuilder();

        for (int i = 2; i < commands.size(); i++) {
          summonerNameBuilder.append(commands.get(i));
        }

        String summonerName = summonerNameBuilder.toString();
        boolean existsSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

        if (!existsSummonerName) {
          String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
          sendErrorMessage(textChannel, errorMessage, Color.RED);
          return;
        }

        LinkedHashMap<Integer, Long> mostChampions = getMostChamp(summonerName, 3);
        List<String> mostChampionsList = Lists.newArrayList();

        for (Map.Entry<Integer, Long> mostChampion : mostChampions.entrySet()) {
          int champId = mostChampion.getKey();
          Champ champ = champRepository.findByKey(champId);
          String championName = champ.getName();

          Long count = mostChampion.getValue();
          List<LoLienParticipant> champs = loLienParticipantRepository
              .findByLoLienSummonerSummonerNameAndChampionId(summonerName, champId);

          long wins = champs
              .stream()
              .map(p -> p.getStats().getWin())
              .filter(w -> w)
              .count();

          long lose = count - wins;

          int winRate = (int) ((double) wins / count * 100);

          String message = String
              .format("%s (%s회, %s승 %s패, 승률 %s)", championName, count, wins, lose, winRate);

          mostChampionsList.add(message);
        }

        if (mostChampionsList.size() > 0) {
          String message = String.format("-- %s님의 모스트 TOP3 챔피언 --", summonerName);
          sendMessage(textChannel, message);

          for (int i = 0; i < mostChampionsList.size(); i++) {
            message = "0" + (i + 1) + ". " + mostChampionsList.get(i);
            sendMessage(textChannel, message);
          }
        } else {
          sendErrorMessage(textChannel, "내전 데이터가 존재하지 않습니다.", Color.RED);
        }
        break;
      default:
    }
  }

  /**
   * addResult.
   * @param textChannel textChannel
   * @param matchId matchId
   * @param entries entries
   */
  public void addResult(TextChannel textChannel, long matchId, String[] entries) {
    for (String summonerName : entries) {
      boolean hasSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

      if (!hasSummonerName) {
        String errorMessage = String
            .format("\"!소환사 등록 %s\" 명령어로 소환사 등록을 먼저 해주시기 바랍니다.", summonerName);
        sendErrorMessage(textChannel, errorMessage, Color.BLUE);
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

      Set<LoLienParticipant> loLienParticipantSet = Sets.newHashSet();
      Set<LoLienTeamStats> loLienTeamStatsSet = Sets.newHashSet();

      LoLienMatch loLienMatch = LoLienMatch
          .builder()
          .gameCreation(gameCreation)
          .gameDuration(gameDuration)
          .gameId(gameId)
          .gameMode(gameMode)
          .gameType(gameType)
          .gameVersion(gameVersion)
          .mapId(mapId)
          .participants(loLienParticipantSet)
          .platformId(platformId)
          .queueId(queueId)
          .seasonId(seasonId)
          .teams(loLienTeamStatsSet)
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

        LoLienParticipantStats loLienParticipantStats = LoLienParticipantStats
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

        LoLienParticipant loLienParticipant = LoLienParticipant
            .builder()
            .match(loLienMatch)
            .championId(championId)
            .participantId(participantId)
            .spell1Id(spell1Id)
            .spell2Id(spell2Id)
            .stats(loLienParticipantStats)
            .teamId(teamId)
            .loLienSummoner(bySummonerName)
            .build();

        loLienParticipantStats.setParticipant(loLienParticipant);

        loLienParticipantSet.add(loLienParticipant);
      }

      List<TeamStats> teams = match.getTeams();
      List<LoLienTeamBans> loLienTeamBansList = Lists.newArrayList();

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

        LoLienTeamStats loLienTeamStats = LoLienTeamStats
            .builder()
            .match(loLienMatch)
            .bans(loLienTeamBansList)
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

          LoLienTeamBans loLienTeamBans = LoLienTeamBans
              .builder()
              .teamStats(loLienTeamStats)
              .championId(championId)
              .pickTurn(pickTurn)
              .build();

          loLienTeamBansList.add(loLienTeamBans);
        }

        loLienTeamStatsSet.add(loLienTeamStats);
      }

      loLienMatchRepository.save(loLienMatch);

      sendMessage(textChannel, "내전 결과가 성공적으로 등록 되었습니다.");

      for (String summonerName : entries) {
        HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
        boolean hasHashKey = opsForHash.hasKey(REDIS_MOST_CHAMPS_KEY, summonerName);
        if (hasHashKey) {
          opsForHash.delete(REDIS_MOST_CHAMPS_KEY, summonerName);
        }
        getMostChamp(summonerName, 3);
      }
    } catch (RiotApiException e) {
      int errorCode = e.getErrorCode();
      if (errorCode == RiotApiException.FORBIDDEN) {
        sendErrorMessage(textChannel,
            "Riot API Key가 만료되어 기능이 정상적으로 작동하지 않습니다."
                + "개발자에게 알려주세요.", Color.RED);
        throw new IllegalArgumentException("api-key-expired");
      } else {
        logger.error("{}", e.getMessage());
        throw new IllegalArgumentException("riotApiException");
      }
    }
  }

  LinkedHashMap<Integer, Long> getMostChamp(String summonerName, int limit) {
    HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
    boolean hasHashKey = opsForHash.hasKey(REDIS_MOST_CHAMPS_KEY, summonerName);

    if (hasHashKey) {
      String mostChampJson = (String) opsForHash.get(REDIS_MOST_CHAMPS_KEY, summonerName);
      return gson.fromJson(mostChampJson, new LinkedIntegerLongHashMapTypeToken().getType());
    } else {
      List<LoLienParticipant> participants = loLienParticipantRepository
          .findByLoLienSummonerSummonerName(summonerName);

      LinkedHashMap<Integer, Long> mostChamps = participants
          .stream()
          .collect(Collectors
              .groupingBy(LoLienParticipant::getChampionId,
                  Collectors.counting()))
          .entrySet()
          .stream()
          .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
          .limit(limit)
          .collect(
              Collectors
                  .toMap(Map.Entry::getKey,
                      Map.Entry::getValue, (oldValue, newValue) -> oldValue,
                      LinkedHashMap::new));

      String mostChampsJson = objectToJsonString(mostChamps);
      opsForHash.put(REDIS_MOST_CHAMPS_KEY, summonerName, mostChampsJson);

      return mostChamps;
    }
  }

  private void sendSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !도움말 명령어를 확인해 주세요.", Color.RED);
  }

  public void sendAddResultSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 접근 입니다.", Color.RED);
  }

  private void sendGetResultSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !내전 결과 조회", Color.RED);
  }
}