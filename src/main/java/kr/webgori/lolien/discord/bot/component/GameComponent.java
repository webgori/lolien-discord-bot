package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getEndDateOfMonth;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getMatch;
import static kr.webgori.lolien.discord.bot.util.CommonUtil.getStartDateOfMonth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.dto.SummonerMostChampDto;
import kr.webgori.lolien.discord.bot.dto.SummonerMostChampsDto;
import kr.webgori.lolien.discord.bot.dto.customgame.AddResultDto;
import kr.webgori.lolien.discord.bot.entity.LolienMatch;
import kr.webgori.lolien.discord.bot.entity.LolienParticipant;
import kr.webgori.lolien.discord.bot.entity.LolienParticipantStats;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.LolienTeamBans;
import kr.webgori.lolien.discord.bot.entity.LolienTeamStats;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.repository.LolienParticipantRepository;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;
import net.rithms.riot.api.endpoints.match.dto.TeamBans;
import net.rithms.riot.api.endpoints.match.dto.TeamStats;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@Component
public class GameComponent {
  private static final String REDIS_MOST_CHAMPS_KEY = "lolien-discord-bot:most-champs";
  private static final int DEFAULT_MOST_CHAMP_COUNT = 3;

  private final LolienSummonerRepository lolienSummonerRepository;
  private final LolienParticipantRepository lolienParticipantRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final AuthenticationComponent authenticationComponent;
  private final HttpServletRequest httpServletRequest;

  /**
   * 리플레이 파일을 byte[]로 변환.
   * @param file file
   * @return byte[]
   */
  public byte[] getReplayBytes(MultipartFile file) {
    if (file == null) {
      return new byte[0];
    }

    try {
      InputStream inputStream = file.getInputStream();
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      logger.error("", e);
    }

    return new byte[0];
  }

  private String getFilename(long gameId) {
    return "KR-" + gameId + ".rofl";
  }

  /**
   * 리플레이 헤더.
   * @param gameId gameId
   * @return HttpHeaders
   */
  public HttpHeaders getReplayHeader(long gameId) {
    HttpHeaders httpHeaders = new HttpHeaders();
    String filename = getFilename(gameId);
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
    return httpHeaders;
  }

  /**
   * checkEntriesSummonerName.
   * @param entries entries
   */
  public void checkEntriesSummonerName(String[] entries) {
    for (String summonerName : entries) {
      String formattedSummonerName = summonerName.replaceAll("\\s+", "")
          .toUpperCase();

      boolean hasSummonerName = lolienSummonerRepository.existsBySummonerName(
          formattedSummonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format("\"%s\" 소환사를 찾을 수 없습니다. "
            + "https://lolien.kr 에서 회원가입 해주세요.", formattedSummonerName);

        throw new IllegalArgumentException(errorMessage);
      }
    }
  }

  /**
   * getNewLolienMatch.
   * @return LolienMatch
   */
  public LolienMatch getNewLolienMatch() {
    Set<LolienParticipant> lolienParticipantSet = Sets.newHashSet();
    Set<LolienTeamStats> lolienTeamStatsSet = Sets.newHashSet();

    return LolienMatch
        .builder()
        .participants(lolienParticipantSet)
        .teams(lolienTeamStatsSet)
        .build();
  }

  /**
   * getNewLolienMatchForUser.
   * @return LolienMatch
   */
  public LolienMatch getNewLolienMatchForUser() {
    Set<LolienParticipant> lolienParticipantSet = Sets.newHashSet();
    Set<LolienTeamStats> lolienTeamStatsSet = Sets.newHashSet();

    LolienMatch lolienMatch = LolienMatch
        .builder()
        .participants(lolienParticipantSet)
        .teams(lolienTeamStatsSet)
        .build();

    User user = getUserFromSession();
    lolienMatch.setUser(user);

    return lolienMatch;
  }

  private User getUserFromSession() {
    Optional<User> userOptional = authenticationComponent.getUser(httpServletRequest);
    return userOptional
        .orElseThrow(() ->
            new BadCredentialsException("내전 결과 등록 중 계정에 문제가 발생하였습니다."));
  }

  /**
   * getAddResultDto.
   * @param lolienMatch lolienMatch
   * @param matchId matchId
   * @param entries entries
   * @return AddResultDto
   */
  public AddResultDto getAddResultDto(LolienMatch lolienMatch, long matchId, String[] entries) {
    Match match = getMatch(matchId);

    return AddResultDto
        .builder()
        .entries(entries)
        .match(match)
        .lolienMatch(lolienMatch)
        .build();
  }

  /**
   * addLolienParticipantSet.
   * @param addResultDto addResultDto
   */
  public void addLolienParticipantSet(AddResultDto addResultDto) {
    List<Participant> participants = addResultDto.getMatch().getParticipants();
    String[] entries = addResultDto.getEntries();
    LolienMatch lolienMatch = addResultDto.getLolienMatch();

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

      addResultDto.getLolienMatch().addParticipant(lolienParticipant);
    }
  }

  /**
   * addLolienTeamStatsSet.
   * @param addResultDto addResultDto
   */
  public void addLolienTeamStatsSet(AddResultDto addResultDto) {
    List<TeamStats> teams = addResultDto.getMatch().getTeams();
    LolienMatch lolienMatch = addResultDto.getLolienMatch();

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

      LolienTeamStats lolienTeamStats = LolienTeamStats
          .builder()
          .match(lolienMatch)
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

      addLolienTeamBansList(teamStats, lolienTeamStats);

      lolienMatch.addTeam(lolienTeamStats);
    }
  }

  private void addLolienTeamBansList(TeamStats teamStats, LolienTeamStats lolienTeamStats) {
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

      lolienTeamStats.addBan(lolienTeamBans);
    }
  }

  /**
   * deleteCustomGameMatchesFromCache.
   */
  public void deleteCustomGameMatchesFromCache() {
    String key = "lolien-discord-bot:custom-game:statistics-%s-%s";
    LocalDate startDateOfMonth = getStartDateOfMonth();
    LocalDate endDateOfMonth = getEndDateOfMonth();
    String redisKey = String.format(key, startDateOfMonth, endDateOfMonth);
    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();
    opsForValue.getOperations().delete(redisKey);
  }

  /**
   * MMR 적용.
   * @param addResultDto addResultDto
   */
  public void addResultMmr(AddResultDto addResultDto) {
    LolienMatch lolienMatch = addResultDto.getLolienMatch();
    Set<LolienParticipant> participants = lolienMatch.getParticipants();

    List<LolienSummoner> team1Summoners = participants
        .stream()
        .filter(a -> a.getTeamId() == 100)
        .map(LolienParticipant::getLolienSummoner)
        .collect(Collectors.toList());

    float team1MmrAverage = (float) team1Summoners
        .stream()
        .mapToInt(LolienSummoner::getMmr)
        .average()
        .orElse(0);

    List<LolienSummoner> team2Summoners = participants
        .stream()
        .filter(a -> a.getTeamId() == 200)
        .map(LolienParticipant::getLolienSummoner)
        .collect(Collectors.toList());

    float team2MmrAverage = (float) team2Summoners
        .stream()
        .mapToInt(LolienSummoner::getMmr)
        .average()
        .orElse(0);

    for (LolienParticipant lolienParticipant : participants) {
      LolienSummoner lolienSummoner = lolienParticipant.getLolienSummoner();
      int afterMmr = getAfterMmr(team1MmrAverage, team2MmrAverage, lolienParticipant);
      lolienSummoner.setMmr(afterMmr);

      addResultDto.addLolienSummoner(lolienSummoner);
    }
  }

  private int getAfterMmr(float team1MmrAverage, float team2MmrAverage,
                          LolienParticipant lolienParticipant) {

    LolienParticipantStats stats = lolienParticipant.getStats();
    Boolean win = stats.getWin();

    int afterMmr;
    LolienSummoner lolienSummoner = lolienParticipant.getLolienSummoner();
    int beforeMmr = lolienSummoner.getMmr();
    float probablyOdds = getProbablyOdds(team1MmrAverage, team2MmrAverage, lolienParticipant);

    if (win) {
      afterMmr = (int) (beforeMmr + (20) * (1 - probablyOdds));
    } else {
      afterMmr = (int) (beforeMmr + (20) * (0 - probablyOdds));
    }

    return afterMmr;
  }

  private float getProbablyOdds(float team1MmrAverage, float team2MmrAverage,
                                LolienParticipant lolienParticipant) {
    int teamId = lolienParticipant.getTeamId();
    float exponent;

    if (teamId == 100) {
      exponent = (team2MmrAverage - team1MmrAverage) / 400;
    } else {
      exponent = (team1MmrAverage - team2MmrAverage) / 400;
    }

    float pow = (float) Math.pow(10, exponent);
    return 1 / (1 +  pow);
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

  /**
   * 리플레이 업로드.
   * @param addResultDto addResultDto
   * @param multipartFile multipartFile
   */
  public void uploadReplay(AddResultDto addResultDto, MultipartFile multipartFile) {
    Long gameId = addResultDto.getLolienMatch().getGameId();
    String filePath = getFilePath(gameId);
    File file = new File(filePath);

    try {
      multipartFile.transferTo(file);
    } catch (IOException e) {
      logger.error("", e);
      boolean delete = file.delete();

      if (!delete) {
        logger.error("리플레이 업로드 - 파일 삭제 실패");
      }
    }
  }

  /**
   * updateMostChampFromCache.
   * @param entries entries
   */
  public void updateMostChampFromCache(String[] entries) {
    ValueOperations<String, Object> opsForValue = redisTemplate.opsForValue();

    for (String summonerName : entries) {
      String formattedSummonerName = summonerName.replaceAll("\\s+", "")
          .toUpperCase();

      String key = String.format("%s:%s", REDIS_MOST_CHAMPS_KEY, formattedSummonerName);
      boolean hasKey = Optional.ofNullable(opsForValue.getOperations().hasKey(key)).orElse(false);

      if (hasKey) {
        opsForValue.getOperations().delete(key);
      }

      getMostChamp(formattedSummonerName);
    }
  }

  /**
   * setLolienMatch.
   * @param addResultDto addResultDto
   */
  public void setLolienMatch(AddResultDto addResultDto) {
    Match match = addResultDto.getMatch();
    LolienMatch lolienMatch = addResultDto.getLolienMatch();
    BeanUtils.copyProperties(match, lolienMatch);
  }

  /**
   * 리플레이 파일 경로.
   * @param gameId gameId
   * @return 파일 경로
   */
  public String getFilePath(Long gameId) {
    return String.format("/media/data/docker/lolien-web/replay/KR-%d.rofl", gameId);
  }
}
