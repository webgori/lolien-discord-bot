package kr.webgori.lolien.discord.bot.component;

import static kr.webgori.lolien.discord.bot.util.CommonUtil.getMatch;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import kr.webgori.lolien.discord.bot.entity.LolienSummoner;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeague;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueMatch;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueParticipant;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueParticipantStats;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueSchedule;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeamBans;
import kr.webgori.lolien.discord.bot.entity.league.LolienLeagueTeamStats;
import kr.webgori.lolien.discord.bot.entity.user.User;
import kr.webgori.lolien.discord.bot.exception.LeagueNotFoundException;
import kr.webgori.lolien.discord.bot.repository.LolienSummonerRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueMatchRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueRepository;
import kr.webgori.lolien.discord.bot.repository.league.LolienLeagueScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rithms.riot.api.endpoints.match.dto.Match;
import net.rithms.riot.api.endpoints.match.dto.Participant;
import net.rithms.riot.api.endpoints.match.dto.ParticipantStats;
import net.rithms.riot.api.endpoints.match.dto.TeamBans;
import net.rithms.riot.api.endpoints.match.dto.TeamStats;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LeagueComponent {
  private final LolienLeagueRepository lolienLeagueRepository;
  private final LolienSummonerRepository lolienSummonerRepository;
  private final LolienLeagueMatchRepository lolienLeagueMatchRepository;
  private final LolienLeagueScheduleRepository lolienLeagueScheduleRepository;
  private final AuthenticationComponent authenticationComponent;
  private final HttpServletRequest httpServletRequest;

  /**
   * addResult.
   *
   * @param leagueIdx leagueIdx
   * @param matchId   matchId
   * @param entries   entries
   */
  public void addResult(int leagueIdx, int scheduleIdx, long matchId, String[] entries) {
    LolienLeague lolienLeague = lolienLeagueRepository
        .findById(leagueIdx)
        .orElseThrow(() -> new LeagueNotFoundException("존재하지 않는 리그 입니다."));

    LolienLeagueSchedule schedule = lolienLeagueScheduleRepository
        .findById(scheduleIdx)
        .orElseThrow(() -> new LeagueNotFoundException("존재하지 않는 대진표 입니다."));

    for (String summonerName : entries) {
      boolean hasSummonerName = lolienSummonerRepository.existsBySummonerName(summonerName);

      if (!hasSummonerName) {
        String errorMessage = String.format("\"%s\" 소환사를 찾을 수 없습니다. "
                        + "https://lolien.kr 에서 회원가입 해주세요.", summonerName);

        throw new IllegalArgumentException(errorMessage);
      }
    }

    Match match = getMatch(matchId);

    Set<LolienLeagueParticipant> lolienLeagueParticipantSet = Sets.newHashSet();
    Set<LolienLeagueTeamStats> lolienLeagueTeamStatsSet = Sets.newHashSet();

    User user = authenticationComponent.getUser(httpServletRequest);

    LolienLeagueMatch lolienLeagueMatch = LolienLeagueMatch
        .builder()
        .lolienLeague(lolienLeague)
        .schedule(schedule)
        .participants(lolienLeagueParticipantSet)
        .teams(lolienLeagueTeamStatsSet)
        .user(user)
        .build();

    BeanUtils.copyProperties(match, lolienLeagueMatch);

    List<Participant> participants = match.getParticipants();

    for (int i = 0; i < participants.size(); i++) {
      Participant participant = participants.get(i);
      ParticipantStats stats = participant.getStats();

      LolienLeagueParticipantStats lolienLeagueParticipantStats = LolienLeagueParticipantStats
          .builder()
          .build();

      BeanUtils.copyProperties(stats, lolienLeagueParticipantStats);

      String summonerName = entries[i];
      String nonSpaceSummonerName = summonerName.replaceAll("\\s+", "");
      LolienSummoner bySummonerName = lolienSummonerRepository
          .findBySummonerName(nonSpaceSummonerName);

      LolienLeagueParticipant lolienLeagueParticipant = LolienLeagueParticipant
          .builder()
          .match(lolienLeagueMatch)
          .stats(lolienLeagueParticipantStats)
          .lolienSummoner(bySummonerName)
          .build();

      BeanUtils.copyProperties(participant, lolienLeagueParticipant);

      lolienLeagueParticipantStats.setParticipant(lolienLeagueParticipant);

      lolienLeagueParticipantSet.add(lolienLeagueParticipant);
    }

    List<TeamStats> teams = match.getTeams();
    List<LolienLeagueTeamBans> lolienLeagueTeamBansList = Lists.newArrayList();

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

      LolienLeagueTeamStats lolienLeagueTeamStats = LolienLeagueTeamStats
          .builder()
          .match(lolienLeagueMatch)
          .bans(lolienLeagueTeamBansList)
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

        LolienLeagueTeamBans lolienLeagueTeamBans = LolienLeagueTeamBans
            .builder()
            .teamStats(lolienLeagueTeamStats)
            .championId(championId)
            .pickTurn(pickTurn)
            .build();

        lolienLeagueTeamBansList.add(lolienLeagueTeamBans);
      }

      lolienLeagueTeamStatsSet.add(lolienLeagueTeamStats);
    }

    lolienLeagueMatchRepository.save(lolienLeagueMatch);
  }
}