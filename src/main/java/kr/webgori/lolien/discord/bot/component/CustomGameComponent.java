package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getMatch;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendErrorMessage;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.dto.SummonerMostChampDto;
import kr.webgori.lolien.discord.bot.dto.SummonerMostChampsDto;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienParticipantStats;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienTeamBans;
import kr.webgori.lolien.discord.bot.entity.LolienTeamStats;
import kr.webgori.lolien.discord.bot.repository.LolienMatchRepository;
import kr.webgori.lolien.discord.bot.repository.LolienParticipantRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomGameComponent {
  private static final String REDIS_MOST_CHAMPS_KEY = "lolien-discord-bot:most-champs";
  private static final int DEFAULT_MOST_CHAMP_COUNT = 3;

  private final LolienSummonerRepository lolienSummonerRepository;
  private final LolienMatchRepository lolienMatchRepository;
  private final LolienParticipantRepository lolienParticipantRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final RiotComponent riotComponent;
  private final CommonComponent commonComponent;
  private final ObjectMapper objectMapper;

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

            List<LolienMatch> matches = lolienMatchRepository
                .findTop5AllByOrderByGameCreationDesc();

            List<String> latestCustomGames = Lists.newArrayList();

            for (LolienMatch lolienMatch : matches) {
              Long gameCreation = lolienMatch.getGameCreation();
              ZoneId zone = ZoneId.systemDefault();
              DateTimeFormatter df = DateTimeFormatter
                  .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);
              String gameCreationDateTime = df.format(Instant.ofEpochMilli(gameCreation));

              Long gameDuration = lolienMatch.getGameDuration();
              Duration duration = Duration.ofSeconds(gameDuration);
              long hour = duration.toHours();
              long minute = duration.toMinutes();

              if (hour > 0) {
                minute = minute - hour * 60;
              }

              Set<LolienParticipant> participants = lolienMatch.getParticipants();
              LolienParticipant lolienParticipant = Collections
                  .max(participants,
                      Comparator.comparing(s -> s.getStats()
                          .getTotalDamageDealtToChampions()));

              LolienSummoner lolienSummoner = lolienParticipant.getLolienSummoner();
              String summonerName = lolienSummoner.getSummonerName();

              LolienParticipantStats stats = lolienParticipant.getStats();
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

            boolean existsByGameId = lolienMatchRepository.existsByGameId(matchId);

            if (!existsByGameId) {
              sendErrorMessage(textChannel, "내전 데이터가 존재하지 않습니다.", Color.RED);
              return;
            }

            lolienMatchRepository.deleteByGameId(matchId);
            break;
          }
          default:
        }
        break;
      case "참여횟수":
        if (commands.size() == 2) {
          Map<String, Integer> top5OfCustomGamePlayCountMaps = Maps.newLinkedHashMap();
          List<LolienParticipant> participants = lolienParticipantRepository.findAll();
          for (LolienParticipant participant : participants) {
            String summonerName = participant.getLolienSummoner().getSummonerName();
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
            boolean existsSummonerName = lolienSummonerRepository
                .existsBySummonerName(summonerName);

            if (!existsSummonerName) {
              String errorMessage = String.format("%s 소환사가 존재하지 않습니다.", summonerName);
              sendErrorMessage(textChannel, errorMessage, Color.RED);
              continue;
            }

            int customGamePlayCount = lolienParticipantRepository
                .findByLolienSummonerSummonerName(summonerName).size();
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

        SummonerMostChampsDto mostChampsDto = getMostChamp(summonerName);
        List<SummonerMostChampDto> mostChampDtoList = mostChampsDto.getMostChamps();
        List<String> mostChampionsList = Lists.newArrayList();

        for (SummonerMostChampDto mostChamp : mostChampDtoList) {
          int champId = mostChamp.getChampionId();
          String championName = riotComponent.getChampionNameByChampId(champId);

          long count = mostChamp.getCount();
          List<LolienParticipant> champs = lolienParticipantRepository
              .findByLolienSummonerSummonerNameAndChampionId(summonerName, champId);

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
          List<LolienSummoner> top5ByOrderByMmrDesc = lolienSummonerRepository
              .findTop5ByOrderByMmrDesc();

          StringBuilder messageBuilder = new StringBuilder();

          for (int i = 0; i < top5ByOrderByMmrDesc.size(); i++) {
            LolienSummoner lolienSummoner = top5ByOrderByMmrDesc.get(i);
            summonerName = lolienSummoner.getSummonerName();
            int mmr = lolienSummoner.getMmr();

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

          LolienSummoner bySummonerName = lolienSummonerRepository.findBySummonerName(summonerName);
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
    boolean existsSummonerName = lolienSummonerRepository.existsBySummonerName(summonerName);

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
      boolean hasSummonerName = lolienSummonerRepository.existsBySummonerName(nonSpaceSummonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format(
            "Discord에서 \"!소환사 등록 %s\" 명령어로 소환사 등록을 먼저 해주시기 바랍니다.",
            nonSpaceSummonerName);

        throw new IllegalArgumentException(errorMessage);
      }
    }

    Match match = getMatch(matchId);

    Set<LolienParticipant> lolienParticipantSet = Sets.newHashSet();
    Set<LolienTeamStats> lolienTeamStatsSet = Sets.newHashSet();

    LolienMatch lolienMatch = LolienMatch
        .builder()
        .participants(lolienParticipantSet)
        .teams(lolienTeamStatsSet)
        .build();

    BeanUtils.copyProperties(match, lolienMatch);

    List<Participant> participants = match.getParticipants();

    for (int i = 0; i < participants.size(); i++) {
      Participant participant = participants.get(i);
      ParticipantStats stats = participant.getStats();

      LolienParticipantStats lolienParticipantStats = LolienParticipantStats
          .builder()
          .build();

      BeanUtils.copyProperties(stats, lolienParticipantStats);

      String summonerName = entries[i];
      String nonSpaceSummonerName = summonerName.replaceAll("\\s+", "");
      LolienSummoner bySummonerName = lolienSummonerRepository
          .findBySummonerName(nonSpaceSummonerName);

      LolienParticipant lolienParticipant = LolienParticipant
          .builder()
          .match(lolienMatch)
          .lolienSummoner(bySummonerName)
          .stats(lolienParticipantStats)
          .build();

      BeanUtils.copyProperties(participant, lolienParticipant);

      lolienParticipantStats.setParticipant(lolienParticipant);

      lolienParticipantSet.add(lolienParticipant);
    }

    List<TeamStats> teams = match.getTeams();
    List<LolienTeamBans> lolienTeamBansList = Lists.newArrayList();

    for (TeamStats teamStats : teams) {
      LolienTeamStats lolienTeamStats = LolienTeamStats
          .builder()
          .match(lolienMatch)
          .bans(lolienTeamBansList)
          .build();

      List<TeamBans> bans = teamStats.getBans();

      for (TeamBans teamBans : bans) {
        int championId = teamBans.getChampionId();
        int pickTurn = teamBans.getPickTurn();

        LolienTeamBans lolienTeamBans = LolienTeamBans
            .builder()
            .teamStats(lolienTeamStats)
            .championId(championId)
            .pickTurn(pickTurn)
            .build();

        lolienTeamBansList.add(lolienTeamBans);
      }

      lolienTeamStatsSet.add(lolienTeamStats);
    }

    lolienMatchRepository.save(lolienMatch);

    addResultMmr(lolienMatch);

    for (String summonerName : entries) {
      ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();

      String nonSpaceSummonerName = summonerName.replaceAll("\\s+", "");
      String key = String.format("%s:%s", REDIS_MOST_CHAMPS_KEY, nonSpaceSummonerName);
      boolean hasKey = Optional.ofNullable(opsForValue.getOperations().hasKey(key)).orElse(false);

      if (hasKey) {
        opsForValue.getOperations().delete(key);
      }

      getMostChamp(nonSpaceSummonerName);
    }
  }

  private void addResultMmr(LolienMatch lolienMatch) {
    Set<LolienParticipant> participants = lolienMatch.getParticipants();

    List<LolienSummoner> team1Summoners = participants
        .stream()
        .filter(a -> a.getTeamId() == 100)
        .map(LolienParticipant::getLolienSummoner)
        .collect(Collectors.toList());

    double team1MmrAverage = team1Summoners
        .stream()
        .mapToInt(LolienSummoner::getMmr)
        .average()
        .orElse(0);

    List<LolienSummoner> team2Summoners = participants
        .stream()
        .filter(a -> a.getTeamId() == 200)
        .map(LolienParticipant::getLolienSummoner)
        .collect(Collectors.toList());

    double team2MmrAverage = team2Summoners
        .stream()
        .mapToInt(LolienSummoner::getMmr)
        .average()
        .orElse(0);

    for (LolienParticipant lolienParticipant : participants) {
      LolienParticipantStats stats = lolienParticipant.getStats();
      Integer teamId = lolienParticipant.getTeamId();

      Boolean win = stats.getWin();
      LolienSummoner lolienSummoner = lolienParticipant.getLolienSummoner();
      int mmr = lolienSummoner.getMmr();

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

        lolienSummoner.plusMmr(resultMmr);
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

        lolienSummoner.minusMmr(resultMmr);
      }

      lolienSummonerRepository.save(lolienSummoner);
    }
  }

  SummonerMostChampsDto getMostChamp(String summonerName) {
    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();

    String key = String.format("%s:%s", REDIS_MOST_CHAMPS_KEY, summonerName);
    boolean hasKey = Optional.ofNullable(opsForValue.getOperations().hasKey(key)).orElse(false);

    if (hasKey) {
      Object obj = redisTemplate.opsForValue().get(key);
      return objectMapper.convertValue(obj, SummonerMostChampsDto.class);
    } else {
      List<LolienParticipant> participants = lolienParticipantRepository
          .findByLolienSummonerSummonerName(summonerName);

      Map<Integer, Long> groupingByChampionId = participants
          .stream()
          .collect(Collectors.groupingBy(LolienParticipant::getChampionId, Collectors.counting()));

      List<SummonerMostChampDto> summonerMostChampDtoList = Lists.newArrayList();

      for (Map.Entry<Integer, Long> groupingByMap : groupingByChampionId.entrySet()) {
        int championId = groupingByMap.getKey();
        long count = groupingByMap.getValue();

        SummonerMostChampDto summonerMostChampDto = SummonerMostChampDto
            .builder()
            .championId(championId)
            .count(count)
            .build();

        summonerMostChampDtoList.add(summonerMostChampDto);
      }

      summonerMostChampDtoList = summonerMostChampDtoList
          .stream()
          .sorted(Comparator.comparing(SummonerMostChampDto::getCount).reversed())
          .limit(DEFAULT_MOST_CHAMP_COUNT)
          .collect(Collectors.toList());

      SummonerMostChampsDto summonerMostChampsDto = SummonerMostChampsDto
          .builder()
          .mostChamps(summonerMostChampDtoList)
          .build();

      redisTemplate.opsForValue().set(key, summonerMostChampsDto);

      return summonerMostChampsDto;
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
