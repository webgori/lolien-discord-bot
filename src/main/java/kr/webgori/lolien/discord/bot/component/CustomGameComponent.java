package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getMatch;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.objectToJsonString;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendErrorMessage;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import java.awt.Color;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.entity.LoLienMatch;
import kr.webgori.lolien.discord.bot.entity.LoLienParticipant;
import kr.webgori.lolien.discord.bot.entity.LoLienParticipantStats;
import kr.webgori.lolien.discord.bot.entity.LoLienSummoner;
import kr.webgori.lolien.discord.bot.entity.LoLienTeamBans;
import kr.webgori.lolien.discord.bot.entity.LoLienTeamStats;
import kr.webgori.lolien.discord.bot.repository.LoLienMatchRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienParticipantRepository;
import kr.webgori.lolien.discord.bot.repository.LoLienSummonerRepository;
import kr.webgori.lolien.discord.bot.spring.LinkedIntegerLongHashMapTypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;
import net.rithms.riot.api.endpoints.match.dto.TeamBans;
import net.rithms.riot.api.endpoints.match.dto.TeamStats;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomGameComponent {
  private static final String REDIS_MOST_CHAMPS_KEY = "lolien-discord-bot:most-champs";
  private static final int DEFAULT_MOST_CHAMP_COUNT = 3;

  private final LoLienSummonerRepository loLienSummonerRepository;
  private final LoLienMatchRepository loLienMatchRepository;
  private final LoLienParticipantRepository loLienParticipantRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final Gson gson;
  private final ChampComponent champComponent;
  private final CommonComponent commonComponent;

  /**
   * execute.
   *
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

    switch (arg1.toUpperCase()) {
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
        if (checkSummonerName(textChannel, summonerName)) {
          return;
        }

        LinkedHashMap<Integer, Long> mostChampions = getMostChamp(summonerName);
        List<String> mostChampionsList = Lists.newArrayList();

        for (Map.Entry<Integer, Long> mostChampion : mostChampions.entrySet()) {
          int champId = mostChampion.getKey();
          String championName = champComponent.getChampionNameByChampId(champId);

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
      case "MMR":
        if (commands.size() == 2) {
          List<LoLienSummoner> top5ByOrderByMmrDesc = loLienSummonerRepository
              .findTop5ByOrderByMmrDesc();

          StringBuilder messageBuilder = new StringBuilder();

          for (int i = 0; i < top5ByOrderByMmrDesc.size(); i++) {
            LoLienSummoner loLienSummoner = top5ByOrderByMmrDesc.get(i);
            summonerName = loLienSummoner.getSummonerName();
            int mmr = loLienSummoner.getMmr();

            String message = String.format("%d. %s (%s)", i + 1, summonerName, mmr);

            messageBuilder.append(message);
            messageBuilder.append("\n");
          }

          sendMessage(textChannel, messageBuilder.toString());
        } else {
          summonerNameBuilder = new StringBuilder();

          for (int i = 2; i < commands.size(); i++) {
            summonerNameBuilder.append(commands.get(i));
          }

          summonerName = summonerNameBuilder.toString();
          if (checkSummonerName(textChannel, summonerName)) {
            return;
          }

          LoLienSummoner bySummonerName = loLienSummonerRepository.findBySummonerName(summonerName);
          commonComponent.checkExistsMmr(bySummonerName);
          int mmr = bySummonerName.getMmr();
          String message = String.format("%s님의 내전 MMR은 %s 입니다.", summonerName, mmr);

          sendMessage(textChannel, message);
        }

        break;
      default:
    }
  }

  private boolean checkSummonerName(TextChannel textChannel, String summonerName) {
    boolean existsSummonerName = loLienSummonerRepository.existsBySummonerName(summonerName);

    if (!existsSummonerName) {
      String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
      sendErrorMessage(textChannel, errorMessage, Color.RED);
      return true;
    }
    return false;
  }

  /**
   * addResult.
   *
   * @param matchId matchId
   * @param entries entries
   */
  public void addResult(long matchId, String[] entries) {
    for (String summonerName : entries) {
      String nonSpaceSummonerName = summonerName.replaceAll("\\s+", "");
      boolean hasSummonerName = loLienSummonerRepository.existsBySummonerName(nonSpaceSummonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format(
            "Discord에서 \"!소환사 등록 %s\" 명령어로 소환사 등록을 먼저 해주시기 바랍니다.",
            nonSpaceSummonerName);

        throw new IllegalArgumentException(errorMessage);
      }
    }

    Match match = getMatch(matchId);

    Set<LoLienParticipant> loLienParticipantSet = Sets.newHashSet();
    Set<LoLienTeamStats> loLienTeamStatsSet = Sets.newHashSet();

    LoLienMatch loLienMatch = LoLienMatch
        .builder()
        .participants(loLienParticipantSet)
        .teams(loLienTeamStatsSet)
        .build();

    BeanUtils.copyProperties(match, loLienMatch);

    List<Participant> participants = match.getParticipants();

    for (int i = 0; i < participants.size(); i++) {
      Participant participant = participants.get(i);
      ParticipantStats stats = participant.getStats();

      LoLienParticipantStats loLienParticipantStats = LoLienParticipantStats
          .builder()
          .build();

      BeanUtils.copyProperties(stats, loLienParticipantStats);

      String summonerName = entries[i];
      String nonSpaceSummonerName = summonerName.replaceAll("\\s+", "");
      LoLienSummoner bySummonerName = loLienSummonerRepository
          .findBySummonerName(nonSpaceSummonerName);

      LoLienParticipant loLienParticipant = LoLienParticipant
          .builder()
          .match(loLienMatch)
          .loLienSummoner(bySummonerName)
          .stats(loLienParticipantStats)
          .build();

      BeanUtils.copyProperties(participant, loLienParticipant);

      loLienParticipantStats.setParticipant(loLienParticipant);

      loLienParticipantSet.add(loLienParticipant);
    }

    List<TeamStats> teams = match.getTeams();
    List<LoLienTeamBans> loLienTeamBansList = Lists.newArrayList();

    for (TeamStats teamStats : teams) {
      LoLienTeamStats loLienTeamStats = LoLienTeamStats
          .builder()
          .match(loLienMatch)
          .bans(loLienTeamBansList)
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

    addResultMmr(loLienMatch);

    for (String summonerName : entries) {
      HashOperations<String, Object, Object> opsForHash = redisTemplate.opsForHash();
      String nonSpaceSummonerName = summonerName.replaceAll("\\s+", "");
      boolean hasHashKey = opsForHash.hasKey(REDIS_MOST_CHAMPS_KEY, summonerName);
      if (hasHashKey) {
        opsForHash.delete(REDIS_MOST_CHAMPS_KEY, nonSpaceSummonerName);
      }
      getMostChamp(nonSpaceSummonerName);
    }
  }

  private void addResultMmr(LoLienMatch loLienMatch) {
    Set<LoLienParticipant> participants = loLienMatch.getParticipants();

    List<LoLienSummoner> team1Summoners = participants
        .stream()
        .filter(a -> a.getTeamId() == 100)
        .map(LoLienParticipant::getLoLienSummoner)
        .collect(Collectors.toList());

    double team1MmrAverage = team1Summoners
        .stream()
        .mapToInt(LoLienSummoner::getMmr)
        .average()
        .orElse(0);

    List<LoLienSummoner> team2Summoners = participants
        .stream()
        .filter(a -> a.getTeamId() == 200)
        .map(LoLienParticipant::getLoLienSummoner)
        .collect(Collectors.toList());

    double team2MmrAverage = team2Summoners
        .stream()
        .mapToInt(LoLienSummoner::getMmr)
        .average()
        .orElse(0);

    for (LoLienParticipant loLienParticipant : participants) {
      LoLienParticipantStats stats = loLienParticipant.getStats();
      Integer teamId = loLienParticipant.getTeamId();

      Boolean win = stats.getWin();
      LoLienSummoner loLienSummoner = loLienParticipant.getLoLienSummoner();
      int mmr = loLienSummoner.getMmr();

      if (win) {
        int resultMmr = 0;

        if (teamId == 100) {
          if (mmr > team2MmrAverage) {
            resultMmr = (int) (mmr / team2MmrAverage * 1);
          } else if (mmr < team2MmrAverage) {
            resultMmr = (int) (team2MmrAverage / mmr * 1.5);
          }
        } else if (teamId == 200) {
          if (mmr > team1MmrAverage) {
            resultMmr = (int) (mmr / team1MmrAverage * 1);
          } else if (mmr < team1MmrAverage) {
            resultMmr = (int) (team1MmrAverage / mmr * 1.5);
          }
        }

        loLienSummoner.plusMmr(resultMmr);
      } else {
        int resultMmr = 0;

        if (teamId == 100) {
          if (mmr > team2MmrAverage) {
            resultMmr = (int) (mmr / team2MmrAverage * 1.5);
          } else if (mmr < team2MmrAverage) {
            resultMmr = (int) (team2MmrAverage / mmr * 1);
          }
        } else if (teamId == 200) {
          if (mmr > team1MmrAverage) {
            resultMmr = (int) (mmr / team1MmrAverage * 1.5);
          } else if (mmr < team1MmrAverage) {
            resultMmr = (int) (team1MmrAverage / mmr * 1);
          }
        }

        loLienSummoner.minusMmr(resultMmr);
      }

      loLienSummonerRepository.save(loLienSummoner);
    }
  }

  LinkedHashMap<Integer, Long> getMostChamp(String summonerName) {
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
          .limit(DEFAULT_MOST_CHAMP_COUNT)
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