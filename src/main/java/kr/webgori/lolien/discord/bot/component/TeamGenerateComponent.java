package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.localDateTimeToString;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendErrorMessage;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.sendMessage;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.stringArrayToStringList;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.stringToLocalDateTime;

import com.google.common.collect.Lists;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import kr.webgori.lolien.discord.bot.config.JdaConfig;
import kr.webgori.lolien.discord.bot.dto.LolienGenerateTeamDto;
import kr.webgori.lolien.discord.bot.dto.SummonerMostChampDto;
import kr.webgori.lolien.discord.bot.dto.SummonerMostChampsDto;
import kr.webgori.lolien.discord.bot.entity.League;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.rithms.riot.api.ApiConfig;
import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameInfo;
import net.rithms.riot.api.endpoints.spectator.dto.CurrentGameParticipant;
import net.rithms.riot.constant.Platform;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class TeamGenerateComponent {
  public static final String CURRENT_SEASON = "S10";
  private static final String DEFAULT_TIER = "UNRANKED";
  private static final String REDIS_GENERATED_TEAM_USERS_INFO_KEY
      = "lolien-discord-bot:generated-team-users-info";
  private static final String REDIS_GENERATED_TEAM_MATCHES_INFO_KEY
      = "lolien-discord-bot:generated-team-matches-info";
  private static final Long LOLIEN_DISCORD_BOT_CUSTOM_GAME_GENERATE_TEAM_CHANNEL_ID
      = 564816760059068445L;

  private final LolienSummonerRepository lolienSummonerRepository;
  private final CustomGameComponent customGameComponent;
  private final RedisTemplate<String, Object> redisTemplate;
  private final RiotComponent riotComponent;

  /**
   * execute.
   *
   * @param event event
   */
  public void execute(MessageReceivedEvent event) {
    TextChannel textChannel = event.getTextChannel();
    String receivedMessage = event.getMessage().getContentDisplay();
    List<String> commands = Lists.newArrayList(receivedMessage.split(" "));

    if (commands.size() < 3) {
      sendSyntax(textChannel);
      return;
    }

    String subCommand = commands.get(1);

    if (!subCommand.equals("밸런스") && !subCommand.equals("랜덤")) {
      sendSyntax(textChannel);
      return;
    }

    StringBuilder entriesBuilder = new StringBuilder();

    for (int i = 2; i < commands.size(); i++) {
      entriesBuilder.append(commands.get(i));
    }

    String[] entries = entriesBuilder.toString().split(",");

    if (entries.length % 5 != 0) {
      sendErrorMessage(textChannel, "참가자가 인원이 잘못되었습니다.", Color.RED);
      return;
    }

    String discordNickname = event.getMember().getEffectiveName();
    String nonSpaceDiscordNickname = discordNickname.replaceAll("\\s+", "");

    boolean existsSummoner = lolienSummonerRepository.existsBySummonerName(nonSpaceDiscordNickname);

    if (!existsSummoner) {
      String errorMessage = String
          .format("디스코드 별명 %s 을 (를) 찾을 수 없습니다. "
              + "https://lolien.kr 에 회원가입 해주시거나 소환사 이름을 맞춰주시기 바랍니다.",
              nonSpaceDiscordNickname);

      sendErrorMessage(textChannel, errorMessage, Color.RED);
      return;
    }

    List<String> entryList = stringArrayToStringList(entries);

    LolienGenerateTeamDto lolienGenerateTeamDto = getLolienGenerateTeamDto(textChannel, entryList);
    String message = getTeamGenerateMessage(lolienGenerateTeamDto);

    LolienSummoner lolienSummoner = lolienSummonerRepository
        .findBySummonerName(nonSpaceDiscordNickname);

    String id = lolienSummoner.getId();

    HashOperations<String, Object, String> hashOperations = redisTemplate.opsForHash();
    LocalDateTime now = LocalDateTime.now();
    String nowString = localDateTimeToString(now);

    hashOperations.put(REDIS_GENERATED_TEAM_USERS_INFO_KEY, id, nowString);

    sendMessage(textChannel, message);
  }

  private LolienGenerateTeamDto getLolienGenerateTeamDto(TextChannel textChannel,
                                                         List<String> entryList) {
    List<LolienGenerateTeamDto> lolienGenerateTeamDtoList = Lists.newArrayList();

    for (int i = 0; i < 50; i++) {
      Collections.shuffle(entryList);

      List<LolienSummoner> team1 = Lists.newArrayList();
      List<LolienSummoner> team2 = Lists.newArrayList();

      for (String summonerName : entryList) {
        checkRegister(textChannel, summonerName);

        LolienSummoner lolienSummoner = lolienSummonerRepository.findBySummonerName(summonerName);

        if (team1.size() < 5) {
          team1.add(lolienSummoner);
        } else {
          team2.add(lolienSummoner);
        }
      }

      float team1Mmr = getTeamMmr(team1);
      float team2Mmr = getTeamMmr(team2);
      float mmrDifference = Math.abs(team1Mmr - team2Mmr);

      LolienGenerateTeamDto lolienGenerateTeamDto = LolienGenerateTeamDto
              .builder()
              .summonersTeam1(team1)
              .summonersTeam2(team2)
              .mmrDifference(mmrDifference)
              .team1Mmr(team1Mmr)
              .team2Mmr(team2Mmr)
              .build();

      lolienGenerateTeamDtoList.add(lolienGenerateTeamDto);
    }

    return lolienGenerateTeamDtoList
        .stream()
        .min(Comparator.comparing(LolienGenerateTeamDto::getMmrDifference))
        .orElseThrow(IllegalArgumentException::new);
  }

  @NotNull
  private String getTeamGenerateMessage(LolienGenerateTeamDto lolienGenerateTeamDto) {
    StringBuilder message = new StringBuilder("1팀: ");

    List<LolienSummoner> summonersTeam1 = lolienGenerateTeamDto.getSummonersTeam1();
    float team1Mmr = lolienGenerateTeamDto.getTeam1Mmr();
    float team2Mmr = lolienGenerateTeamDto.getTeam2Mmr();

    message = getTeamGenerateMessageByTeam(message, summonersTeam1, team1Mmr, team2Mmr);

    message.append("\n\n");

    message.append("2팀: ");

    List<LolienSummoner> summonersTeam2 = lolienGenerateTeamDto.getSummonersTeam2();

    return getTeamGenerateMessageByTeam(message, summonersTeam2, team2Mmr, team1Mmr).toString();
  }

  @NotNull
  private StringBuilder getTeamGenerateMessageByTeam(StringBuilder message,
                                                     List<LolienSummoner> teamSummoners,
                                                     float teamMmr,
                                                     float enemyTeamMmr) {

    for (LolienSummoner summoner : teamSummoners) {
      String summonerName = summoner.getSummonerName();
      message.append(summonerName);

      String tier = summoner
          .getLeagues()
          .stream()
          .filter(l -> l.getSeason().equals(CURRENT_SEASON))
          .findAny()
          .orElseGet(() -> League.builder().tier(DEFAULT_TIER).build())
          .getTier();

      int winResultMmr = getMmrGap(summoner, teamMmr, enemyTeamMmr, true);
      int loseResultMmr = getMmrGap(summoner, teamMmr, enemyTeamMmr, false);
      String mmrInfo = String.format("+%s, -%s", winResultMmr, loseResultMmr);
      String summonerInfo = String.format(" (%s, %s)", tier, mmrInfo);
      message.append(summonerInfo);

      message.append(", ");
    }

    message = new StringBuilder(message.substring(0, message.length() - 2));

    String teamMmrMessage = String.format(" - 평균 MMR %s", teamMmr);
    message.append(teamMmrMessage);

    message.append("\n");

    List<String> teamSummonerName = teamSummoners
        .stream()
        .map(LolienSummoner::getSummonerName)
        .collect(Collectors.toList());

    String teamSummonerMostTop3 = setTeamSummonerMostTop3(teamSummonerName);
    message.append(teamSummonerMostTop3);
    return message;
  }

  int getMmrGap(LolienSummoner lolienSummoner, float teamMmr, float enemyTeamMmr, boolean win) {
    int beforeMmr = lolienSummoner.getMmr();
    float probablyOdds = getProbablyOdds(teamMmr, enemyTeamMmr);

    if (win) {
      return (int) (beforeMmr + (20) * (1 - probablyOdds)) - beforeMmr;
    } else {
      return beforeMmr - (int) (beforeMmr + (20) * (0 - probablyOdds));
    }
  }

  private float getProbablyOdds(float teamMmr, float enemyTeamMmr) {
    float exponent = (enemyTeamMmr - teamMmr) / 400;
    float pow = (float) Math.pow(10, exponent);
    return 1 / (1 +  pow);
  }

  private float getTeamMmr(List<LolienSummoner> team1) {
    return (float) team1
        .stream()
        .mapToInt(LolienSummoner::getMmr)
        .average()
        .orElse(0);
  }

  private void checkRegister(TextChannel textChannel, String summonerName) {
    LolienSummoner lolienSummoner = lolienSummonerRepository.findBySummonerName(summonerName);

    checkSummonerRegister(textChannel, summonerName, lolienSummoner);
    checkUserRegister(textChannel, summonerName, lolienSummoner);
  }

  private void checkSummonerRegister(TextChannel textChannel, String summonerName,
                                               LolienSummoner lolienSummoner) {

    if (Objects.isNull(lolienSummoner)) {
      String errorMessage = String
          .format("\"%s\" 소환사를 찾을 수 없습니다. "
              + "https://lolien.kr 에서 회원가입 해주세요.", summonerName);
      sendErrorMessage(textChannel, errorMessage, Color.BLUE);
      throw new IllegalArgumentException("register summoner first");
    }
  }

  private void checkUserRegister(TextChannel textChannel, String summonerName,
                                 LolienSummoner lolienSummoner) {

    User user = lolienSummoner.getUser();

    if (Objects.isNull(user)) {
      String errorMessage = String
          .format("\"%s\" 소환사는 https://lolien.kr 회원가입이 되어 있지 않습니다. "
              + "회원가입을 해주시기 바랍니다.", summonerName);

      sendErrorMessage(textChannel, errorMessage, Color.BLUE);
      throw new IllegalArgumentException("register summoner first");
    }
  }

  private String setTeamSummonerMostTop3(List<String> entryList) {
    StringBuilder stringBuilder = new StringBuilder();

    for (String summoner : entryList) {
      SummonerMostChampsDto mostChampsDto = customGameComponent.getMostChamp(summoner);
      List<SummonerMostChampDto> mostChampDtoList = mostChampsDto.getMostChamps();
      List<String> mostChampionsList = Lists.newArrayList();

      for (SummonerMostChampDto mostChampDto : mostChampDtoList) {
        int champId = mostChampDto.getChampionId();
        String championName = riotComponent.getChampionNameByChampId(champId);
        mostChampionsList.add(championName);
      }

      String mostChamps = String.join(", ", mostChampionsList);
      String mostSummonerChamps = String.format("%s (%s)", summoner, mostChamps);
      stringBuilder
          .append("\n")
          .append(mostSummonerChamps);
    }

    return stringBuilder.toString();
  }

  private void sendSyntax(TextChannel textChannel) {
    sendErrorMessage(textChannel, "잘못된 명령어 입니다. !팀구성 밸런스 소환사명1, 소환사명2, 소환사명3 ...", Color.RED);
  }

  @Scheduled(cron = "0 */1 * ? * *")
  private void checkActiveGame() {
    HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
    Set<Object> ids = hashOperations.keys(REDIS_GENERATED_TEAM_USERS_INFO_KEY);

    for (Object id : ids) {
      Optional.ofNullable(hashOperations.get(REDIS_GENERATED_TEAM_USERS_INFO_KEY, id))
          .ifPresent(s -> {
            long between = ChronoUnit.MINUTES.between(stringToLocalDateTime((String) s),
                LocalDateTime.now());

            if (between >= 30) {
              deleteGeneratedTeamUsersInfo(hashOperations, id);
            } else if (between >= 10) {
              getActiveGameBySummoner((String) id)
                  .ifPresent(currentGameInfo -> {
                    List<CurrentGameParticipant> participants = currentGameInfo.getParticipants();

                    List<String> summonerIds = participants
                        .stream()
                        .map(CurrentGameParticipant::getSummonerId)
                        .collect(Collectors.toList());

                    long existsTotalSummonerCount = lolienSummonerRepository
                        .countByIdIn(summonerIds);

                    // 소환사 등록한 사용자가 6명 이상이면 (내전이면)
                    if (existsTotalSummonerCount > 5) {
                      long gameId = currentGameInfo.getGameId();

                      Boolean hasKey = hashOperations
                          .hasKey(REDIS_GENERATED_TEAM_MATCHES_INFO_KEY, String.valueOf(gameId));

                      if (!hasKey) {
                        String summonersName = currentGameInfo
                            .getParticipants()
                            .stream()
                            .map(CurrentGameParticipant::getSummonerName)
                            .collect(Collectors.joining(","));

                        hashOperations.put(
                            REDIS_GENERATED_TEAM_MATCHES_INFO_KEY, String.valueOf(gameId),
                            summonersName);

                        TextChannel textChannel = JdaConfig
                            .getJda()
                            .getTextChannelById(
                                LOLIEN_DISCORD_BOT_CUSTOM_GAME_GENERATE_TEAM_CHANNEL_ID);

                        String message = "Lolien 내전이 시작되었습니다.";
                        sendMessage(textChannel, message);
                      }
                    }
                    deleteGeneratedTeamUsersInfo(hashOperations, id);
                  });
            }
          });
    }
  }

  private void deleteGeneratedTeamUsersInfo(HashOperations<String, Object, Object> hashOperations,
                                            Object id) {
    hashOperations.delete(REDIS_GENERATED_TEAM_USERS_INFO_KEY, id);
  }

  @Scheduled(cron = "0 */1 * ? * *")
  private void checkEndGame() {
    HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
    Set<Object> ids = hashOperations.keys(REDIS_GENERATED_TEAM_MATCHES_INFO_KEY);

    for (Object id : ids) {
      long matchId = Long.parseLong((String) id);
      Optional<Match> matchOptional = getMatchByMatchId(matchId);
      matchOptional.flatMap(match -> Optional.ofNullable(hashOperations
          .get(REDIS_GENERATED_TEAM_MATCHES_INFO_KEY, String.valueOf(id)))).ifPresent(a -> {

            String[] summonersName = ((String) a).split(",");

            if (summonersName.length > 0) {
              customGameComponent.addResult(matchId, summonersName);
            }

            hashOperations.delete(REDIS_GENERATED_TEAM_MATCHES_INFO_KEY, String.valueOf(matchId));
          });
    }
  }

  private RiotApi getRiotApi() {
    String riotApiKey = ConfigComponent.getRiotApiKey();
    ApiConfig config = new ApiConfig().setKey(riotApiKey);
    return new RiotApi(config);
  }

  private Optional<CurrentGameInfo> getActiveGameBySummoner(String id) {
    try {
      RiotApi riotApi = getRiotApi();
      return Optional.of(riotApi.getActiveGameBySummoner(Platform.KR, id));
    } catch (RiotApiException e) {
      return Optional.empty();
    }
  }

  private Optional<Match> getMatchByMatchId(long matchId) {
    RiotApi riotApi = getRiotApi();
    try {
      return Optional.ofNullable(riotApi.getMatch(Platform.KR, matchId));
    } catch (RiotApiException e) {
      return Optional.empty();
    }
  }
}
